/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2008-2009,2012-2015,2017,2019-2021 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2017 Ruud Poutsma <rtimon@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.game;

import java.io.Serializable;
import java.util.Arrays;

import static soc.game.SOCResourceConstants.*;

/**
 * This represents a collection of
 * clay, ore, sheep, wheat, and wood resources.
 * Unknown resources are also tracked here.
 * Although it's possible to store negative amounts of resources, it's discouraged.
 *
 * @see SOCResourceConstants
 * @see SOCPlayingPiece#getResourcesToBuild(int)
 */
@SuppressWarnings("serial")
public class SOCResourceSet implements ResourceSet, Serializable, Cloneable
{
    /** Resource set with zero of each resource type */
    public static final SOCResourceSet EMPTY_SET = new SOCResourceSet();

    public transient final Object semaphore = new Object();

    /**
     * the number of each resource type.
     * Indexes 1 to n are used:
     * 1 == {@link SOCResourceConstants#CLAY},
     * 2 == {@link SOCResourceConstants#ORE},
     * ...
     * 5 == {@link SOCResourceConstants#WOOD},
     * 6 == {@link SOCResourceConstants#UNKNOWN}.
     */
    private int[] resources;

    /**
     * Make an empty resource set
     */
    public SOCResourceSet()
    {
        resources = new int[MAXPLUSONE];
        clear();
    }

    /**
     * Make a resource set with stuff in it
     *
     * @param cl  number of clay resources
     * @param or  number of ore resources
     * @param sh  number of sheep resources
     * @param wh  number of wheat resources
     * @param wo  number of wood resources
     * @param uk  number of unknown resources
     */
    public SOCResourceSet(int cl, int or, int sh, int wh, int wo, int uk)
    {
        resources = new int[MAXPLUSONE];

        resources[CLAY]    = cl;
        resources[ORE]     = or;
        resources[SHEEP]   = sh;
        resources[WHEAT]   = wh;
        resources[WOOD]    = wo;
        resources[UNKNOWN] = uk;
    }

    /**
     * Make a resource set from an array
     *
     * @param rset resource set, of length 5 or 6 (clay, ore, sheep, wheat, wood, unknown).
     *     If length is 5, unknown == 0.
     * @see #getAmounts(boolean)
     * @since 1.1.08
     */
    public SOCResourceSet(int[] rset)
    {
        // Note that rset[]'s indexes are different from resources[]'s indexes.

        this(rset[0], rset[1], rset[2], rset[3], rset[4], (rset.length >= 6) ? rset[5] : 0);
    }

    /**
     * Construct a new resource set from an immutable resource set (copy constructor)
     * @param other instance to copy contents from
     *
     * @implNote This constructor does not support {@link SOCResourceConstants#UNKNOWN}
     */
    public SOCResourceSet(ResourceSet other)
    {
        this();
        resources[CLAY] = other.getAmount(CLAY);
        resources[ORE] = other.getAmount(ORE);
        resources[SHEEP] = other.getAmount(SHEEP);
        resources[WHEAT] = other.getAmount(WHEAT);
        resources[WOOD] = other.getAmount(WOOD);
        resources[UNKNOWN] = other.getAmount(UNKNOWN);
    }

    /**
     * set the number of resources to zero
     * @see #isEmpty()
     */
    public void clear()
    {
        Arrays.fill(resources, 0);
    }

    /**
     * Is this set empty, containing zero resources?
     * @return true if set is completely empty, including its amount of unknown resources
     * @see #getTotal()
     * @see #clear()
     * @since 2.4.50
     */
    public boolean isEmpty()
    {
        for (int resource : resources)
            if (resource != 0)
                return false;

        return true;
    }

    /**
     * Does the set contain any resources of this type?
     * @param resourceType  the type of resource, like {@link SOCResourceConstants#CLAY}
     *     or {@link SOCResourceConstants#UNKNOWN}
     * @return true if the set's amount of this resource &gt; 0
     * @since 2.0.00
     * @see #getAmount(int)
     * @see #contains(ResourceSet)
     * @see #isEmpty()
     */
    public boolean contains(final int resourceType)
    {
        if (resourceType >= resources.length)
            return false;
        return (resources[resourceType] > 0);
    }

    /**
     * How many resources of this type are contained in the set?
     * @param resourceType  the type of resource, like {@link SOCResourceConstants#CLAY}
     *     or {@link SOCResourceConstants#UNKNOWN}
     * @return the number of a kind of resource
     * @see #contains(int)
     * @see #getTotal()
     * @see #getAmounts(boolean)
     * @see #isEmpty()
     */
    public int getAmount(int resourceType)
    {
        return resources[resourceType];
    }

    /**
     * How many resources of each type are contained in the set?
     * (<tt>{@link SOCResourceConstants#CLAY}, ORE, SHEEP, WHEAT, WOOD</tt>)
     * @param withUnknown  If true, also include the amount of {@link SOCResourceConstants#UNKNOWN} resources
     * @return the amounts of each known resource in the set,
     *    starting with {@link SOCResourceConstants#CLAY} at index 0, up to {@link SOCResourceConstants#WOOD WOOD} at 4.
     *    If {@code withUnknown}, index 5 is the amount of {@link SOCResourceConstants#UNKNOWN}.
     * @see #getAmount(int)
     * @see #isEmpty()
     * @see #SOCResourceSet(int[])
     * @since 2.0.00
     */
    public int[] getAmounts(final boolean withUnknown)
    {
        final int limit = (withUnknown) ? UNKNOWN : WOOD;  // 5 or 6, searchable for where-used
        int[] amt = new int[limit];
        for (int i = 0, res = CLAY; i <limit; ++i, ++res)
            amt[i] = resources[res];

        return amt;
    }

    /**
     * Get the total number of resources in this set, including unknown types.
     * @return the total number of resources
     * @see #getKnownTotal()
     * @see #getAmount(int)
     * @see #getAmounts(boolean)
     * @see #getResourceTypeCount()
     * @see #isEmpty()
     */
    public int getTotal()
    {
        int sum = 0;

        for (int i = MIN;
                 i < MAXPLUSONE; i++)
        {
            sum += resources[i];
        }

        return sum;
    }

    /**
     * Get the number of known resource types contained in this set:
     * {@link SOCResourceConstants#CLAY} to {@link SOCResourceConstants#WOOD},
     * excluding {@link SOCResourceConstants#UNKNOWN} or {@link SOCResourceConstants#GOLD_LOCAL}.
     * An empty set returns 0, a set containing only wheat returns 1,
     * that same set after adding wood and sheep returns 3, etc.
     * @return  The number of resource types in this set with nonzero resource counts.
     * @see #isEmpty()
     * @since 2.0.00
     */
    public int getResourceTypeCount()
    {
        int typ = 0;

        for (int i = MIN;
                 i <= WOOD; ++i)
        {
            if (resources[i] != 0)
                ++typ;
        }

        return typ;
    }

    /**
     * Get the total amount of resources of known types:
     * {@link SOCResourceConstants#CLAY} to {@link SOCResourceConstants#WOOD},
     * excluding {@link SOCResourceConstants#UNKNOWN} or {@link SOCResourceConstants#GOLD_LOCAL}.
     * @return the total number of known-type resources
     * @see #isEmpty()
     * @since 1.1.14
     */
    public int getKnownTotal()
    {
        int sum = 0;

        for (int i = MIN;
                 i <= WOOD; i++)
        {
            sum += resources[i];
        }

        return sum;
    }

    /**
     * Set the amount of a resource.
     * To set all resources from another set, use {@link #add(SOCResourceSet)},
     * {@link #subtract(ResourceSet)} or {@link #setAmounts(SOCResourceSet)}.
     *
     * @param rtype the type of resource, like {@link SOCResourceConstants#CLAY}
     * @param amt   the amount
     */
    public void setAmount(int amt, int rtype)
    {
        resources[rtype] = amt;
    }

    /**
     * add an amount to a resource
     *
     * @param rtype the type of resource, like {@link SOCResourceConstants#CLAY}
     *     or {@link SOCResourceConstants#UNKNOWN}
     * @param amt   the amount; if below 0 (thus subtracting resources),
     *              the subtraction occurs and no special action is taken.
     *              {@link #subtract(int, int)} takes special action in some cases.
     */
    public void add( int amt, int rtype )
    {
        synchronized( semaphore )
        {
            resources[rtype] += amt;
        }
    }

    /**
     * subtract an amount from a resource.
     *<P>
     * If we're subtracting more from a known resource than there are of that resource,
     * sets that resource to 0, then takes the "excess" away from this set's
     * {@link SOCResourceConstants#UNKNOWN} resources.
     * As a result, {@code UNKNOWN} field may be less than 0 afterwards.
     *<P>
     * To convert all known resources to {@code UNKNOWN} when subtracting an {@code UNKNOWN} rtype,
     * call {@link #subtract(int, int, boolean)} instead.
     *
     * @param rtype the type of resource, like {@link SOCResourceConstants#CLAY}
     *     or {@link SOCResourceConstants#UNKNOWN}
     * @param amt   the amount; unlike in {@link #add(int, int)}, any amount that
     *              takes the resource below 0 is treated specially.
     */
    public boolean subtract(int amt, int rtype)
    {
        return subtract(amt, rtype, false);
    }

    /**
     * Subtract an amount from a resource.
     * <p>
     *     In theory, every player (i.e. robot) knows about all the cards in other players' hands
     *     by tracking resources from dice rolls. Any resources obtained through "secret" means
     *     (e.g. robbery) are stored as {@link SOCResourceConstants#UNKNOWN} types.
     * </p>
     * <p>
     *     If we're subtracting more of a known resource than there are of that resource,
     *     sets that resource to 0, then takes the "excess" amount away from this set's
     *     {@link SOCResourceConstants#UNKNOWN} resources. The number of unknown resources should
     *     never be less than zero; that means we've screwed up the count somehow.
     * </p>
     * <p>
     *     Should unknowns become less than zero we should probably reset the count by asking the
     *     server for a total count from this player, subtract the count of known resources, then
     *     storing the remainder as unknowns.
     * </p>
     * <P>
     * If {@code asUnknown} is true and we're subtracting an {@code UNKNOWN} rtype,
     * converts this set's known resources to {@code UNKNOWN} before subtraction
     * by calling {@link #convertAllToUnknown()}.
     *
     * @param rtype the type of resource, like {@link SOCResourceConstants#CLAY}
     *     or {@link SOCResourceConstants#UNKNOWN}
     * @param amt   the amount; unlike in {@link #add(int, int)}, any amount that
     *     takes the resource below 0 is treated specially
     * @param asUnknown  If true and subtracting {@link SOCResourceConstants#UNKNOWN},
     *     calls {@link #convertAllToUnknown()} first
     * @since 2.4.50
     */
    public boolean subtract(final int amt, final int rtype, final boolean asUnknown)
    {
        synchronized (semaphore)
        {
            int[] savedResources = resources.clone();
            if (0 < amt && asUnknown && (rtype == UNKNOWN))
            {
                convertAllToUnknown();
                resources[UNKNOWN] -= amt;
            }
            else
            {
                final int ourAmt = resources[rtype];
                if (amt > ourAmt)
                {
                    // if more than we know about is being removed, some of our unknowns must be
                    // that type. If we don't have that many unknowns, then something has screwed
                    // up; convert everything to unknown then subtract from that.
                    int excess = amt - ourAmt;
                    if (resources[UNKNOWN] >= excess )
                    {
                        resources[UNKNOWN] -= excess;
                        resources[rtype] = 0;
                    }
                    else
                    {
                        convertAllToUnknown();
                        resources[UNKNOWN] -= amt;
                    }
                }
                else
                {
                    resources[rtype] -= amt;
                }
            }

            if (resources[UNKNOWN] < 0)
            {
                // This should never happen. If it does, send a signal to the calling routine
                // to reset the set from the server
                System.err.println( String.format( "[%d] UKNOWN == %d : RESOURCE TYPE == %d %s",
                    Thread.currentThread().getId(), resources[UNKNOWN], rtype, Arrays.toString( savedResources ) ) );
                convertAllToUnknown();
                return false;
            }
        }
        return true;
    }

    /**
     * add an entire resource set's amounts into this set.
     *
     * @param toAdd  the resource set
     */
    public void add(SOCResourceSet toAdd)
    {
        synchronized (semaphore)
        {
            resources[CLAY] += toAdd.getAmount( CLAY );
            resources[ORE] += toAdd.getAmount( ORE );
            resources[SHEEP] += toAdd.getAmount( SHEEP );
            resources[WHEAT] += toAdd.getAmount( WHEAT );
            resources[WOOD] += toAdd.getAmount( WOOD );
            resources[UNKNOWN] += toAdd.getAmount( UNKNOWN );
        }
    }

    /**
     * subtract an entire resource set.
     *<P>
     * Loops for each resource type in {@code toReduce}, including {@link SOCResourceConstants#UNKNOWN}.
     * If any type's amount would go below 0, clips it to 0.
     * Treats {@code UNKNOWN} no differently than the known types.
     *<P>
     * To instead subtract such "excess" amounts from this set's {@code UNKNOWN} field
     * like {@link #subtract(int, int, boolean)} does,
     * call {@link #subtract(ResourceSet, boolean)}.
     *
     * @param toReduce  the resource set to subtract
     */
    public void subtract(ResourceSet toReduce)
    {
        subtract(toReduce, false);
    }

    // TODO: preventing unknown resources from becoming negative may be a mistake.
    //   review again in the future.
    private void reduce( int resouceType, int amount, boolean fromUnknown )
    {
        resources[ resouceType ] -= amount; // might result in a negative amount
        if (resources[resouceType] < 0 )
        {
            if (fromUnknown)
            {
                resources[UNKNOWN] += resources[resouceType]; // remember, this is a negative value, so adding is subtracting...
                if (0 > resources[UNKNOWN])
                {
                    resources[UNKNOWN] = 0; // We've tried to subtract more unknowns than we have.
                }
            }
            resources[resouceType] = 0; // in any case, we've got no more of this.
        }
    }

    /**
     * subtract an entire resource set.
     *<P>
     * If {@code asUnknown} is true:
     *<UL>
     * <LI> If subtracting {@link SOCResourceConstants#UNKNOWN},
     *      first converts this set's known resources to {@code UNKNOWN}
     *      by calling {@link #convertAllToUnknown()}.
     * <LI> Loops for each resource type in {@code toReduce}, including {@link SOCResourceConstants#UNKNOWN}.
     *      If any known type's amount would go below 0, clips it to 0 and subtracts the "excess"
     *      from the {@code UNKNOWN} field, which can become less than 0.
     *</UL>
     * If false, behaves like {@link #subtract(ResourceSet)}.
     *
     * @param toReduce  the resource set to subtract
     * @param asUnknown  If true: Removes excess amounts from this set's {@link SOCResourceConstants#UNKNOWN}
     *     field instead of clipping to 0; if subtracting {@code UNKNOWN},
     *     calls {@link #convertAllToUnknown() this.convertAllToUnknown()} first
     * @since 2.4.50
     */
    public void subtract(final ResourceSet toReduce, final boolean asUnknown)
    {
        final int amountReduceUnknown = toReduce.getAmount(UNKNOWN);

        synchronized (semaphore)
        {
            if (asUnknown && (amountReduceUnknown > 0))
                convertAllToUnknown();
            reduce( CLAY, toReduce.getAmount( CLAY ), asUnknown );
            reduce( ORE, toReduce.getAmount( ORE ), asUnknown );
            reduce( SHEEP, toReduce.getAmount( SHEEP ), asUnknown );
            reduce( WHEAT, toReduce.getAmount( WHEAT ), asUnknown );
            reduce( WOOD, toReduce.getAmount( WOOD ), asUnknown );

            resources[UNKNOWN] -= amountReduceUnknown;
            if ((resources[UNKNOWN] < 0))
            {
                resources[UNKNOWN] = 0;
            }
        }
    }

    /**
     * Convert all these resources to type {@link SOCResourceConstants#UNKNOWN}.
     * Information on amount of wood, wheat, etc is no longer available.
     * Equivalent to:
     * <code>
     *    int numTotal = resSet.getTotal();
     *    resSet.clear();
     *    resSet.setAmount(UNKNOWN, numTotal);
     * </code>
     * @since 1.1.00
     */
    private void convertAllToUnknown()
    {
        synchronized (semaphore)
        {
            int numTotal = getTotal();
            clear();
            resources[UNKNOWN] = numTotal;
        }
    }

    /**
     *
     * @param other the other SOCResourceSet that is less than or equal to this one.
     * @return true if this SOCResourceSet has a greater count of every type resource than
     * the other set, or if the other set is null. If we have a negative unknown count (which
     * should never happen) assume that our unknowns are larger.
     */
    boolean gte( ResourceSet other )
    {
        if (other == null)
            return true;

        return (resources[CLAY] >= other.getAmount( CLAY ))
            && (resources[ORE] >= other.getAmount( ORE ))
            && (resources[SHEEP] >= other.getAmount( SHEEP ))
            && (resources[WHEAT] >= other.getAmount( WHEAT ))
            && (resources[WOOD] >= other.getAmount( WOOD ))
            && (   (resources[UNKNOWN] < 0)
                || (resources[UNKNOWN] >= other.getAmount( UNKNOWN )));
    }
    /**
     * Human-readable form of the set, with format "clay=5|ore=1|sheep=0|wheat=0|wood=3|unknown=0"
     * @return a human readable longer form of the set
     * @see #toShortString()
     * @see #toFriendlyString()
     */
    public String toString()
    {

        return "clay=" + resources[CLAY]
            + "|ore=" + resources[ORE]
            + "|sheep=" + resources[SHEEP]
            + "|wheat=" + resources[WHEAT]
            + "|wood=" + resources[WOOD]
            + "|unknown=" + resources[UNKNOWN];
    }

    /**
     * Human-readable form of the set, with format "Resources: 5 1 0 0 3 0".
     * Order of types is Clay, ore, sheep, wheat, wood, unknown.
     * @return a human readable short form of the set
     * @see #toFriendlyString()
     */
    public String toShortString()
    {

        return "Resources: " + resources[CLAY] + " "
            + resources[ORE] + " "
            + resources[SHEEP] + " "
            + resources[WHEAT] + " "
            + resources[WOOD] + " "
            + resources[UNKNOWN];
    }

    /**
     * Human-readable form of the set, with format "5 clay,1 ore,3 wood".
     * Unknown resources aren't mentioned.
     * @return a human readable longer form of the set;
     *         if the set is empty, return the string "nothing".
     * @see #toShortString()
     * @since 1.1.00
     */
    public String toFriendlyString()
    {
        StringBuffer sb = new StringBuffer();
        if (toFriendlyString(sb))
            return sb.toString();
        else
            return "nothing";
    }

    /**
     * Human-readable form of the set, with format "5 clay, 1 ore, 3 wood".
     * Unknown resources aren't mentioned.
     * @param sb Append into this buffer.
     * @return true if anything was appended, false if sb unchanged (this resource set is empty).
     * @see #toFriendlyString()
     * @since 1.1.00
     */
    public boolean toFriendlyString( StringBuffer sb )
    {
        boolean needComma = false;  // Has a resource already been appended to sb?
        int amt;

        for (int res = CLAY; res <= UNKNOWN; ++res)
        {
            amt = resources[res];
            if (amt == 0)
                continue;

            if (needComma)
                sb.append(", ");
            sb.append(amt);
            sb.append(" ");
            sb.append(resName(res));
            needComma = true;
        }

        return needComma;  // Did we append anything?
    }

    /**
     * {@inheritDoc}
     * @see #contains(int[])
     */
    public boolean contains( ResourceSet other )
    {
        return gte( other );
    }

    /**
     * Does this set contain all resources of another set?
     * @param other resource set to test against, of length 5 (clay, ore, sheep, wheat, wood) or 6 (with unknown),
     *    or {@code null} for an empty resource subset.
     * @return true if this set contains at least the resource amounts in {@code other}
     *     for each of its resource types. True if {@code other} is null or empty.
     * @throws IllegalArgumentException if a non-null {@code other}'s length is not 5 or 6
     */
    public boolean contains( final int[] other )
        throws IllegalArgumentException
    {
        if (other == null)
            return true;
        if ((other.length != 5) && (other.length != 6))
            throw new IllegalArgumentException("other");

        for (int rtype = CLAY; rtype <= WOOD; ++rtype)
            if (resources[rtype] < other[rtype - 1])
                return false;
        if ((other.length == 6) && (resources[UNKNOWN] < other[5]))
            return false;

        return true;
    }

    /**
     * @return true if the argument is a SOCResourceSet containing the same amounts of each resource, including UNKNOWN
     *
     * @param anObject  the object in question
     */
    public boolean equals(Object anObject)
    {
        if ((anObject instanceof SOCResourceSet)
                && (((SOCResourceSet) anObject).getAmount( CLAY )    == resources[CLAY])
                && (((SOCResourceSet) anObject).getAmount( ORE )     == resources[ORE])
                && (((SOCResourceSet) anObject).getAmount( SHEEP )   == resources[SHEEP])
                && (((SOCResourceSet) anObject).getAmount( WHEAT )   == resources[WHEAT])
                && (((SOCResourceSet) anObject).getAmount( WOOD )    == resources[WOOD])
                && (((SOCResourceSet) anObject).getAmount( UNKNOWN ) == resources[UNKNOWN]))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @return a hashcode for this data, from resource amounts
     */
    public int hashCode()
    {
        return Arrays.hashCode(resources);
    }

    /**
     * Make a copy of this resource set.
     * To instead copy another set into this one, use {@link #setAmounts(SOCResourceSet)}.
     * @return a copy of this resource set
     */
    public SOCResourceSet copy()
    {
        SOCResourceSet copy = new SOCResourceSet();
        copy.add(this);

        return copy;
    }

    /**
     * copy a resource set into this one. This one's current data is lost and overwritten.
     *
     * @param set  the set to copy from
     */
    public void setAmounts(SOCResourceSet set)
    {
        System.arraycopy(set.resources, 0, resources, 0, resources.length);
    }

}
