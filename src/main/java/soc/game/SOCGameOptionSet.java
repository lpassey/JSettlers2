/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Portions of this file Copyright (C) 2020-2021 Jeremy D Monin <jeremy@nand.net>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import soc.communication.SOCClientData;
import soc.server.savegame.SavedGameModel;  // for javadocs only
import soc.util.SOCFeatureSet;
import soc.util.Version;

import static soc.game.SOCGameOption.FLAG_DROP_IF_UNUSED;  // for convenience in getAllKnownOptions()

/**
 * A set of {@link SOCGameOption}s, either those of a game,
 * or all possible Known Options at a server or client: {@link soc.server.SOCServerGameOptionSet#getAllKnownOptions()}.
 *<P>
 * Internally this is a {@code Map} whose keys are the options' {@link SOCVersionedItem#key}s.
 *<P>
 * Before v2.4.50 such sets were represented by <tt>Map&lt;String, SOCGameOption&gt;</tt>
 * and these methods were part of {@link SOCGameOption}.
 * So, some methods here are marked <tt>@since</tt> earlier version numbers.
 * Many classes still use the Map format for simplicity:
 * Use {@link #getAll()} for a lightweight Map of the set.
 *
 *<H3>Known Options</H3>
 *
 * Methods to work with a set of Known Options:
 *
 *<H4>Synchronizing options between server/client versions</H4>
 *<UL>
 * <LI> {@link #optionsForVersion(int)}
 * <LI> {@link #optionsNewerThanVersion(int, boolean, boolean)}
 * <LI> {@link #adjustOptionsToKnown(SOCGameOptionSet, boolean, SOCFeatureSet,Map)}
 * <LI> {@link SOCGameOption#packKnownOptionsToString(SOCGameOptionSet, boolean, boolean)}
 * <LI> {@link SOCGameOption#parseOptionsToSet(String, SOCGameOptionSet)}
 * <LI> {@link SOCGameOption#getMaxIntValueForVersion(String, int)}
 * <LI> {@link SOCGameOption#getMaxEnumValueForVersion(String, int)}
 * <LI> {@link SOCGameOption#trimEnumForVersion(SOCGameOption, int)}
 *</UL>
 *
 *<H4>Individual Options</H4>
 *<UL>
 * <LI> {@link #getKnownOption(String, boolean)}
 * <LI> {@link #addKnownOption(SOCGameOption)}
 * <LI> {@link #setKnownOptionCurrentValue(SOCGameOption)}
 *</UL>
 *
 *<H4>Options available only when Activated or when client has a Feature</H4>
 *<UL>
 * <LI> {@link #optionsNotSupported(SOCFeatureSet)}
 * <LI> {@link #optionsTrimmedForSupport(SOCFeatureSet)}
 * <LI> {@link #optionsWithFlag(int, int)}
 * <LI> {@link #activate(String)}
 *</UL>
 *
 * @author Jeremy D. Monin &lt;jeremy@nand.net&gt;
 * @since 2.4.50
 */
public class SOCGameOptionSet
    implements Iterable<SOCGameOption>
{

    // Some game option keynames, for convenient reference in code and javadocs:

    // -- Game option keynames for scenario flags --
    // Not all scenario keynames have scenario events, some are just properties of the game.

    /**
     * Scenario key <tt>_SC_SANY</tt> for {@link SOCPlayerEvent#SVP_SETTLED_ANY_NEW_LANDAREA}.
     * @since 2.0.00
     */
    public static final String K_SC_SANY = "_SC_SANY";

    /**
     * Scenario key <tt>_SC_SEAC</tt> for {@link SOCPlayerEvent#SVP_SETTLED_EACH_NEW_LANDAREA}.
     * @since 2.0.00
     */
    public static final String K_SC_SEAC = "_SC_SEAC";

    /**
     * Scenario key <tt>_SC_FOG</tt> for {@link SOCGameEvent#SGE_FOG_HEX_REVEALED}.
     * @see SOCScenario#K_SC_FOG
     * @since 2.0.00
     */
    public static final String K_SC_FOG = "_SC_FOG";

    /**
     * Scenario key <tt>_SC_0RVP</tt>: No "longest trade route" VP / Longest Road.
     * @since 2.0.00
     */
    public static final String K_SC_0RVP = "_SC_0RVP";

    /**
     * Scenario key <tt>_SC_3IP</tt>: Third initial placement of settlement and road or ship.
     * Initial resources are given for this one, not the second settlement.
     * @since 2.0.00
     */
    public static final String K_SC_3IP = "_SC_3IP";

    /**
     * Scenario key <tt>_SC_CLVI</tt> for {@link SOCPlayerEvent#CLOTH_TRADE_ESTABLISHED_VILLAGE}:
     * Cloth Trade with neutral {@link SOCVillage villages}.
     * Villages and cloth are in a game only if this option is set.
     * @since 2.0.00
     * @see SOCScenario#K_SC_CLVI
     */
    public static final String K_SC_CLVI = "_SC_CLVI";

    /**
     * Scenario key <tt>_SC_PIRI</tt> for Pirate Islands and {@link SOCFortress Fortresses}.
     * Fortresses and player warships are in a game only if this option is set.
     * For more details and special rules see {@link SOCScenario#K_SC_PIRI}.
     * @since 2.0.00
     */
    public static final String K_SC_PIRI = "_SC_PIRI";

    /**
     * Scenario key {@code _SC_FTRI} for the Forgotten Tribe.
     * Special edges with SVP, dev cards, and "gift" ports placed via {@link SOCInventoryItem}.
     * For more details and special rules see {@link SOCScenario#K_SC_FTRI}.
     * @since 2.0.00
     */
    public static final String K_SC_FTRI = "_SC_FTRI";

    /**
     * Scenario key {@code _SC_WOND} for Wonders.
     * Special unique "wonders" claimed by players and built up to several levels. No pirate ship.
     * For more details, special rules, and {@link SOCSpecialItem Special Item}s, see {@link SOCScenario#K_SC_WOND}.
     * @since 2.0.00
     */
    public static final String K_SC_WOND = "_SC_WOND";

    // -- End of scenario flag keynames --

    /**
     * Inactive boolean game option {@code "PLAY_FO"}:
     * All player info is fully observable. If activated and true,
     * server announces all resource and dev card details with actual types, not "unknown".
     * Useful for developers. Minimum client version 2.0.00.
     * @see #K_PLAY_VPO
     * @since 2.4.50
     */
    public static final String K_PLAY_FO = "PLAY_FO";

    /**
     * Inactive boolean game option {@code "PLAY_VPO"}:
     * All player VP/card info is observable. If activated and true,
     * server announces all dev card details with actual types, not "unknown".
     * Useful for developers. Minimum client version 2.0.00.
     * @see #K_PLAY_FO
     * @since 2.4.50
     */
    public static final String K_PLAY_VPO = "PLAY_VPO";

    // -- Extra option keynames --

    /**
     * An "extra" option key {@code _EXT_BOT} available for robot development.
     * Available for third-party bot developers: Not used by JSettlers core itself.
     * Can hold a string of data which is sent to all robot clients joining a game,
     * entered on the server command line or properties file. A third-party bot might
     * want to use this option's value to configure its behavior or debug settings.
     * Maximum length of this option's value is {@link SOCGameOption#TEXT_OPTION_MAX_LENGTH}.
     * @see #K__EXT_CLI
     * @see #K__EXT_GAM
     * @since 2.0.00
     */
    public static final String K__EXT_BOT = "_EXT_BOT";

    /**
     * An "extra" option key {@code _EXT_CLI} available for client development.
     * Available for third-party developers: Not used by JSettlers core itself.
     * Can hold a string of data which is sent to all clients,
     * entered on the server command line or properties file.
     * Maximum length of this option's value is {@link SOCGameOption#TEXT_OPTION_MAX_LENGTH}.
     * @see #K__EXT_BOT
     * @see #K__EXT_GAM
     * @since 2.0.00
     */
    public static final String K__EXT_CLI = "_EXT_CLI";

    /**
     * An "extra" option key {@code _EXT_GAM} available for game development.
     * Available for third-party developers: Not used by JSettlers core itself.
     * Can hold a string of data which is sent to the game at all clients,
     * entered on the server command line or properties file.
     * Maximum length of this option's value is {@link SOCGameOption#TEXT_OPTION_MAX_LENGTH}.
     * @see #K__EXT_BOT
     * @see #K__EXT_CLI
     * @since 2.0.00
     */
    public static final String K__EXT_GAM = "_EXT_GAM";

    // -- End of extra option keynames --

    /**
     * The options within this set; never {@code null}.
     */
    private final Map<String, SOCGameOption> options;

    /**
     * Create a new empty set.
     */
    public SOCGameOptionSet()
    {
        options = new HashMap<>();
    }

    /**
     * Create an independent copy or deep copy of another set.
     * @param opts  Set to copy; not null
     * @param deepCopy If true also clone each {@link SOCGameOption} in the set,
     *     instead of doing a shallow copy to a new set with to those same option references
     * @throws NullPointerException if {@code opts} is null
     */
    public SOCGameOptionSet( final SOCGameOptionSet opts, final boolean deepCopy )
        throws NullPointerException
    {
        if (deepCopy)
        {
            options = new HashMap<>();
            try
            {
                for (final SOCGameOption opt : opts.options.values())
                    options.put( opt.key, (SOCGameOption) opt.clone() );
            }
            catch( CloneNotSupportedException e )
            {
                // catch required, but not expected to ever happen
                throw new IllegalStateException( "clone failed" );
            }
        }
        else
        {
            options = new HashMap<>( opts.options );
        }
    }

    /**
     * Create a set which contains options.
     * @param opts  Options to include, or null to make an empty set
     */
    public SOCGameOptionSet( final Map<String, SOCGameOption> opts )
    {
        options = (opts != null) ? new HashMap<>( opts ) : new HashMap<String, SOCGameOption>();
    }

    /**
     * Get this set's size (number of options).
     * @return number of {@link SOCGameOption}s currently in this set
     * @see #isEmpty()
     * @see #clear()
     */
    public int size()
    {
        return options.size();
    }

    /**
     * Is this set empty, containing 0 options?
     * @return true if {@link #size()} is 0
     * @see #clear()
     */
    public boolean isEmpty()
    {
        return options.isEmpty();
    }

    /**
     * Does the set contain an option with {@link SOCVersionedItem#key opt.key}?
     * @param opt  Option to look for by key; not null
     * @return True if set contains an option with this {@link SOCVersionedItem#key opt.key}
     * @throws NullPointerException if {@code opt} is null
     * @see #containsKey(String)
     * @see #get(String)
     */
    public boolean contains( final SOCGameOption opt )
        throws NullPointerException
    {
        return options.containsKey( opt.key );
    }

    /**
     * Does the set contain an option with this key?
     * @param optKey  Option key to look for
     * @return True if set contains an option with this {@link SOCVersionedItem#key}
     * @see #contains(SOCGameOption)
     * @see #get(String)
     */
    public boolean containsKey( final String optKey )
    {
        return options.containsKey( optKey );
    }

    /**
     * Add this option to the set, replacing any previous option there
     * with the same {@link SOCVersionedItem#key opt.key}.
     * If you need the replaced previous option, call {@link #put(SOCGameOption)} instead.
     * @param opt  Option to add by key; not null. May be any type, including {@link SOCGameOption#OTYPE_UNKNOWN}.
     * @return True if set didn't already contain an option with that key
     * @throws NullPointerException if {@code opt} is null
     * @see #remove(String)
     * @see #addKnownOption(SOCGameOption)
     */
    public boolean add( final SOCGameOption opt )
    {
        return (options.put( opt.key, opt ) == null);
    }

    /**
     * Put this option into the set, replacing any previous option there
     * with the same {@link SOCVersionedItem#key opt.key}.
     * If you don't need the previous option, you can call {@link #add(SOCGameOption)} instead.
     * @param opt  Option to add by key; not null. May be any type, including {@link SOCGameOption#OTYPE_UNKNOWN}.
     * @return Previously contained option with that key, or {@code null} if none
     * @throws NullPointerException if {@code opt} is null
     * @see #get(String)
     * @see #remove(String)
     * @see #addKnownOption(SOCGameOption)
     */
    public SOCGameOption put( final SOCGameOption opt )
    {
        return options.put( opt.key, opt );
    }

    /**
     * Get the {@link SOCGameOption} in the set having this key, if any.
     * @param optKey  Option key to get
     * @return The option in this set having {@link SOCVersionedItem#key} == {@code optKey},
     *     or {@code null} if none
     * @see #getKnownOption(String, boolean)
     * @see #containsKey(String)
     * @see #getAll()
     */
    public SOCGameOption get( final String optKey )
    {
        return options.get( optKey );
    }

    /**
     * Get all options in the set, as a convenient Map backed by the set; treat as read-only.
     * For simplicity, many classes use that Map format instead of SOCGameOptionSet.
     * @return Map of options in the set, or an empty Map
     * @see #keySet()
     * @see #values()
     * @see soc.server.SOCServerGameOptionSet#getAllKnownOptions()
     */
    public Map<String, SOCGameOption> getAll()
    {
        return options;  // for performance, skip copying to a new Map
    }

    /**
     * Get all options in the set. This collection is backed by the option set,
     * and supports iteration like {@link Map#values()}.
     * @return {@link SOCGameOption}s in the set, or an empty Collection
     * @see #keySet()
     * @see #getAll()
     */
    public Collection<SOCGameOption> values()
    {
        return options.values();
    }

    /**
     * Get all keys in the set. This set of keys is backed by the option set,
     * and supports iteration and element removal like {@link Map#keySet()}.
     * @return Option keys in the set, from each {@link SOCVersionedItem#key}, or an empty Set
     * @see #values()
     * @see #getAll()
     */
    public Set<String> keySet()
    {
        return options.keySet();
    }

    /**
     * For use in {@code for} loops, make and return an iterator;
     * calls {@link Map#values() map.values()}{@link Collection#iterator() .iterator()}.
     * {@link Iterator#remove()} is supported.
     */
    public Iterator<SOCGameOption> iterator()
    {
        return options.values().iterator();
    }

    /**
     * Remove the {@link SOCGameOption} in the set having this key, if any, and return it.
     * @param optKey  Option key to remove
     * @return The option removed from this set which has {@link SOCVersionedItem#key} == {@code optKey},
     *     or {@code null} if none was removed
     * @see #clear()
     */
    public SOCGameOption remove( final String optKey )
    {
        return options.remove( optKey );
    }

    /**
     * Remove all options from this set. Will be empty afterwards.
     * @see #isEmpty()
     * @see #remove(String)
     */
    public void clear()
    {
        options.clear();
    }

    // Examining and updating values within the set:

    /**
     * Is this boolean-valued game option currently set to true?
     *<P>
     * Before v2.4.50 this method was {@code SOCGame.isGameOptionSet(opts, optKey)}.
     *
     * @param optKey Name of a {@link SOCGameOption} of type {@link SOCGameOption#OTYPE_BOOL OTYPE_BOOL},
     *     {@link SOCGameOption#OTYPE_INTBOOL OTYPE_INTBOOL} or {@link SOCGameOption#OTYPE_ENUMBOOL OTYPE_ENUMBOOL}
     * @return True if option's boolean value is set, false if not set or not defined in this set of options
     * @see #setBoolOption(String, SOCGameOptionSet)
     * @see #getOptionIntValue(String)
     * @see #getOptionStringValue(String)
     * @since 1.1.07
     */
    public boolean isOptionSet( final String optKey )
    {
        // OTYPE_* - if a new type is added and it uses a boolean field, update this method's javadoc.

        SOCGameOption op = options.get( optKey );
        if (op == null)
            return false;
        return op.getBoolValue();
    }

    /**
     * Within this set, include a boolean option and make it true.
     * If the option object isn't already in the set, it will be cloned from {@code knownOpts}.
     * @param boKey   Key name for boolean option to set
     * @param knownOpts  Set of Known Options, if needed for adding the option
     * @throws NullPointerException  if {@code boKey} isn't in the set and doesn't exist in {@code knownOpts}
     * @see #isOptionSet(String)
     * @see #setIntOption(String, int, boolean, SOCGameOptionSet)
     * @since 1.1.17
     */
    public void setBoolOption( final String boKey, final SOCGameOptionSet knownOpts )
        throws NullPointerException
    {
        SOCGameOption opt = options.get( boKey );
        if (opt == null)
        {
            opt = knownOpts.getKnownOption( boKey, true );
            opt.setBoolValue( true );
            options.put( boKey, opt );
        }
        else
        {
            opt.setBoolValue( true );
        }
    }

    /**
     * Within this set, include an int or intbool option and set its value.
     * If the option object doesn't exist in this set, it will be cloned from {@code knownOpts}.
     * @param ioKey   Key name for int option to set
     * @param ivalue  Set option to this int value
     * @param bvalue  Set option to this boolean value (ignored if option type not intbool)
     * @param knownOpts  Set of Known Options, if needed for adding the option
     * @throws NullPointerException  if {@code ioKey} isn't in the set and doesn't exist in {@code knownOpts}
     * @see #getOptionIntValue(String)
     * @see #setBoolOption(String, SOCGameOptionSet)
     * @since 1.1.17
     */
    public void setIntOption
    ( final String ioKey, final int ivalue, final boolean bvalue, final SOCGameOptionSet knownOpts )
        throws NullPointerException
    {
        SOCGameOption opt = options.get( ioKey );
        if (opt == null)
        {
            opt = knownOpts.getKnownOption( ioKey, true );
            opt.setIntValue( ivalue );
            opt.setBoolValue( bvalue );
            options.put( ioKey, opt );
        }
        else
        {
            opt.setIntValue( ivalue );
            opt.setBoolValue( bvalue );
        }
    }

    /**
     * What is this integer game option's current value?
     *<P>
     * Does not reference {@link SOCGameOption#getBoolValue()}, only the int value,
     * so this will return a value even if the bool value is false.
     *
     * @param optKey A {@link SOCGameOption} of type {@link SOCGameOption#OTYPE_INT OTYPE_INT},
     *     {@link SOCGameOption#OTYPE_INTBOOL OTYPE_INTBOOL},
     *     {@link SOCGameOption#OTYPE_ENUM OTYPE_ENUM}
     *     or {@link SOCGameOption#OTYPE_ENUMBOOL OTYPE_ENUMBOOL}
     * @return Option's current {@link SOCGameOption#getIntValue() intValue},
     *     or 0 if not defined in the set of options;
     *     OTYPE_ENUM's and _ENUMBOOL's choices give an intVal in range 1 to n.
     * @see #isOptionSet(String)
     * @see #getOptionIntValue(String, int, boolean)
     * @see #getOptionStringValue(String)
     * @since 1.1.07
     */
    public int getOptionIntValue( final String optKey )
    {
        // OTYPE_* - if a new type is added, update this method's javadoc.

        return getOptionIntValue( optKey, 0, false );
    }

    /**
     * What is this integer game option's current value?
     *<P>
     * Can optionally reference {@link SOCGameOption#getBoolValue()}, not only the int value.
     *
     * @param optKey A {@link SOCGameOption} of type {@link SOCGameOption#OTYPE_INT OTYPE_INT},
     *     {@link SOCGameOption#OTYPE_INTBOOL OTYPE_INTBOOL},
     *     {@link SOCGameOption#OTYPE_ENUM OTYPE_ENUM}
     *     or {@link SOCGameOption#OTYPE_ENUMBOOL OTYPE_ENUMBOOL}
     * @param defValue  Default value to use if <tt>optKey</tt> not defined
     * @param onlyIfBoolSet  Check the option's {@link SOCGameOption#getBoolValue()} too;
     *     if false, return <tt>defValue</tt>.
     *     Do not set this parameter if the type doesn't use a boolean component.
     * @return Option's current {@link SOCGameOption#getIntValue() intValue},
     *     or <tt>defValue</tt> if not defined in the set of options;
     *     OTYPE_ENUM's and _ENUMBOOL's choices give an intVal in range 1 to n.
     * @see #isOptionSet(String)
     * @see #getOptionIntValue(String)
     * @see #getOptionStringValue(String)
     * @since 1.1.14
     */
    public int getOptionIntValue
    ( final String optKey, final int defValue, final boolean onlyIfBoolSet )
    {
        // OTYPE_* - if a new type is added, update this method's javadoc.

        SOCGameOption op = options.get( optKey );
        if (op == null)
            return defValue;
        if (onlyIfBoolSet && !op.getBoolValue())
            return defValue;
        return op.getIntValue();
    }

    /**
     * What is this string game option's current value?
     *
     * @param optKey A {@link SOCGameOption} of type
     *     {@link SOCGameOption#OTYPE_STR OTYPE_STR}
     *     or {@link SOCGameOption#OTYPE_STRHIDE OTYPE_STRHIDE}
     * @return Option's current {@link SOCGameOption#getStringValue() getStringValue}
     *     or null if not defined in this set of options
     * @see #isOptionSet(String)
     * @see #getOptionIntValue( String )
     * @since 1.1.07
     */
    public String getOptionStringValue( final String optKey )
    {
        // OTYPE_* - if a new type is added, update this method's javadoc.

        SOCGameOption op = options.get( optKey );
        if (op == null)
            return null;
        return op.getStringValue();
    }

    // For Known Options:

    /**
     * Get information about a known option. See {@link soc.server.SOCServerGameOptionSet#getAllKnownOptions()}
     * for a summary of each known option.
     * Will return the info if known, even if option has {@link SOCGameOption#FLAG_INACTIVE_HIDDEN}.
     *<P>
     * Before v2.4.50 this method was {@code SOCGameOption.getOption(key, clone)}.
     *
     * @param key  Option key
     * @param clone  True if a copy of the option is needed; set this true
     *               unless you're sure you won't be changing any fields of
     *               its original object, which is a shared copy in a static namekey->object map.
     * @return information about a known option, or null if none with that key
     * @throws IllegalStateException  if {@code clone} but the object couldn't be cloned; this isn't expected to ever happen
     * @see #addKnownOption(SOCGameOption)
     * @see #setKnownOptionCurrentValue(SOCGameOption)
     * @since 1.1.07
     */
    public SOCGameOption getKnownOption( final String key, final boolean clone )
        throws IllegalStateException
    {
        SOCGameOption op;
        synchronized (options)
        {
            op = options.get( key );
        }
        if (op == null)
            return null;

        if (clone)
        {
            try
            {
                op = (SOCGameOption) op.clone();
            }
            catch( CloneNotSupportedException ce )
            {
                // required, but not expected to happen
                throw new IllegalStateException( "Clone failed!", ce );
            }
        }

        return op;
    }

    /**
     * Add a new known option (presumably received from a server of newer or older version),
     * or update the option's information.
     * @param onew New option, or a changed version of an option we already know.
     *     If onew.optType == {@link SOCGameOption#OTYPE_UNKNOWN}, will remove from this Known set.
     *     If this option is already known and the old copy has a {@link SOCGameOption#getChangeListener()},
     *     that listener is copied to {@code onew}.
     * @return true if it's new, false if we already had that key and it was updated
     * @see soc.server.SOCServerGameOptionSet#getKnownOption(String, boolean)
     * @see #setKnownOptionCurrentValue(SOCGameOption)
     * @since 1.1.07
     */
    public boolean addKnownOption( final SOCGameOption onew )
    {
        final String oKey = onew.key;
        final boolean hadOld;

        synchronized (options)
        {
            final SOCGameOption oldcopy = options.remove( oKey );
            hadOld = (oldcopy != null);

            if (onew.optType != SOCGameOption.OTYPE_UNKNOWN)
            {
                if (hadOld)
                {
                    final SOCGameOption.ChangeListener cl = oldcopy.getChangeListener();
                    if (cl != null)
                        onew.addChangeListener( cl );
                }

                options.put( oKey, onew );
            }
        }

        return !hadOld;
    }

    // Comparison and synchronization, Known Options:

    /**
     * In a set of Known Options, activate an "inactive" known option:
     * Drop its {@link SOCGameOption#FLAG_INACTIVE_HIDDEN} flag
     * and add {@link SOCGameOption#FLAG_ACTIVATED}. Does nothing if already activated.
     * See {@link SOCGameOption} class javadoc for more about Inactive Options.
     *<P>
     * Since {@link SOCGameOption#optFlags} field is {@code final}, copies to a new option object with updated flags,
     * replacing the old one in the set of known options.
     *<P>
     * To get the list of currently activated options compatible with a certain client version,
     * call {@link #optionsWithFlag(int, int) knownOpts.optionsWithFlag(FLAG_ACTIVATED, cliVersion)}.
     *<P>
     * At the server, activate needed options before any clients connect.
     * Do so by editing/overriding {@link soc.server.SOCServer#serverUp()} to call this method,
     * or setting property {@link soc.server.SOCServer#PROP_JSETTLERS_GAMEOPTS_ACTIVATE}.
     *
     * @param optKey  Known game option's alphanumeric keyname
     * @throws IllegalArgumentException if {@code optKey} isn't a known game option, or if that option
     *     has neither {@link SOCGameOption#FLAG_INACTIVE_HIDDEN} nor {@link SOCGameOption#FLAG_ACTIVATED}
     */
    public void activate( final String optKey )
        throws IllegalArgumentException
    {
        synchronized (options)
        {
            final SOCGameOption orig = options.get( optKey );
            if (orig == null)
                throw new IllegalArgumentException( "unknown: " + optKey );

            if (!orig.hasFlag( SOCGameOption.FLAG_INACTIVE_HIDDEN ))
            {
                if (orig.hasFlag( SOCGameOption.FLAG_ACTIVATED ))
                    return;

                throw new IllegalArgumentException( "not inactive: " + optKey );
            }

            options.put( optKey, new SOCGameOption
                ( (orig.optFlags | SOCGameOption.FLAG_ACTIVATED) & ~SOCGameOption.FLAG_INACTIVE_HIDDEN, orig ) );
        }
    }

    /**
     * In a set of Known Options, set the current value(s) of an option based on the current value(s) of
     * another object {@code ocurr} with the same {@link SOCVersionedItem#key key}.
     * If there is no known option with oCurr.{@link SOCVersionedItem#key key}, it is ignored and nothing is set.
     * @param ocurr Option with the requested current value.
     *     {@code ocurr}'s value field contents are copied to the known option's values,
     *     the {@code ocurr} reference won't be added to the known option set.
     * @throws IllegalArgumentException if string value is not permitted; note that
     *     int values outside of range are silently clipped, and will not throw this exception.
     * @see #getKnownOption(String, boolean)
     * @since 1.1.07
     */
    public void setKnownOptionCurrentValue( SOCGameOption ocurr )
        throws IllegalArgumentException
    {
        final String oKey = ocurr.key;

        synchronized (options)
        {
            final SOCGameOption oKnown = options.get( oKey );

            if (oKnown == null)
                return;

            switch (oKnown.optType)  // OTYPE_*
            {
            case SOCGameOption.OTYPE_BOOL:
                oKnown.setBoolValue( ocurr.getBoolValue() );
                break;

            case SOCGameOption.OTYPE_INT:
            case SOCGameOption.OTYPE_ENUM:
                oKnown.setIntValue( ocurr.getIntValue() );
                break;

            case SOCGameOption.OTYPE_INTBOOL:
            case SOCGameOption.OTYPE_ENUMBOOL:
                oKnown.setBoolValue( ocurr.getBoolValue() );
                oKnown.setIntValue( ocurr.getIntValue() );
                break;

            case SOCGameOption.OTYPE_STR:
            case SOCGameOption.OTYPE_STRHIDE:
                oKnown.setStringValue( ocurr.getStringValue() );
                break;
            }
        }
    }

    /**
     * Compare a set of options against the specified version.
     * Make a list of all which are new or changed since that version.
     *<P>
     * This method has 2 modes, because it's called for 2 different purposes:
     *<UL>
     * <LI> sync client-server Known Option set info, in general: <tt>checkValues</tt> == false
     * <LI> check if client can create game with a specific set of option values: <tt>checkValues</tt> == true,
     *     call on game's proposed Set of game opts instead of Known Opts.
     *    <BR>
     *     Before calling this method, server should call
     *     {@link #adjustOptionsToKnown(SOCGameOptionSet, boolean, SOCFeatureSet,Map)}
     *     to help validate option values.
     *</UL>
     * See <tt>checkValues</tt> for method's behavior in each mode.
     *<P>
     * <B>Game option names:</B><br>
     * When running this at the client (<tt>vers</tt> is the older remote server's version),
     * some of the returned too-new options have long names that can't be sent to a v1.x.xx
     * server (<tt>vers</tt> &lt; {@link SOCGameOption#VERSION_FOR_LONGER_OPTNAMES}).
     * You must check for this and remove them before sending them to the remote server.
     * Game option names sent to 1.x.xx servers must be 3 characters or less, alphanumeric, no underscores ('_').
     *<P>
     * When running at the server, we will never send an option whose name is invalid to v1.x.xx clients,
     * because the SOCGameOption constructors enforce <tt>minVers >= 2000</tt> when the name is longer than 3
     * characters or contains '_'.
     *
     * @param vers  Version to compare known options against
     * @param checkValues  Which mode: Check options' current values and {@link SOCGameOption#minVersion},
     *     not their {@link SOCGameOption#lastModVersion}?  An option's minimum version
     *     can increase based on its value; see {@link SOCGameOption#getMinVersion(Map)}.
     *     <P>
     *     If false, returns list of any game options to send to older server or client {@code vers}.
     *     Ignores any option with {@link SOCGameOption#FLAG_INACTIVE_HIDDEN}.
     *     Adds all options with {@link SOCGameOption#FLAG_ACTIVATED}
     *     having {@link SOCVersionedItem#minVersion minVersion} &lt;= {@code vers},
     *     ignoring their {@link SOCVersionedItem#lastModVersion lastModVersion}
     *     in case activation is the opt's only recent change.
     *     <BR>
     *     If {@code checkValues} and {@code trimEnums} both false, assumes is a client-side call
     *     and also adds any opts with {@link SOCGameOption#FLAG_3RD_PARTY} to the returned list.
     *     <P>
     *     If true, any returned items are in this Set but too new for client {@code vers}:
     *     Game creation should be rejected.
     *     Does not check {@link SOCGameOption#FLAG_INACTIVE_HIDDEN} in this mode; use
     *     {@link SOCGameOptionSet#adjustOptionsToKnown(SOCGameOptionSet, boolean, SOCFeatureSet,Map)} for that check.
     * @param trimEnums  For enum-type options where minVersion changes based on current value,
     *     should we remove too-new values from the returned option info?
     *     This lets us send only the permitted values to an older client.
     *     Also trims int-type options' max value where needed (example: {@code "PL"}).
     * @return List of the newer (added or changed) {@link SOCGameOption}s, or null
     *     if all are known and unchanged since <tt>vers</tt>.
     *     <BR>
     *     <B>Note:</B> May include options with {@link SOCGameOption#minVersion} &gt; {@code vers}
     *     if client has asked about them by name.
     * @see #optionsForVersion(int)
     * @see #optionsTrimmedForSupport(SOCFeatureSet)
     * @since 1.1.07
     */
    public List<SOCGameOption> optionsNewerThanVersion
    ( final int vers, final boolean checkValues, final boolean trimEnums )
    {
        return implOptionsVersionCheck( vers, false, checkValues, trimEnums );
    }

    /**
     * In a set of Known Options, get all options valid at version {@code vers}.
     * If necessary, trims enum value ranges or int value ranges if range was smaller at {@code vers},
     * like {@link #optionsNewerThanVersion(int, boolean, boolean)} does.
     *<P>
     * If {@code vers} from a client is newer than this version of SOCGameOption, will return all options known at this
     * version, which may not include all of the newer version's options.  Client game-option negotiation handles this
     * by having the newer client send all its new (added or changed) option keynames to the older server to allow,
     * adjust, or reject.
     *<P>
     * Will omit any option that has {@link SOCGameOption#FLAG_INACTIVE_HIDDEN}.
     *
     * @param vers  Version to compare options against
     * @return List of all {@link SOCGameOption}s valid at version {@code vers}, or {@code null} if none.
     * @see #optionsNewerThanVersion(int, boolean, boolean)
     * @see #optionsTrimmedForSupport(SOCFeatureSet)
     * @since 2.0.00
     */
    public List<SOCGameOption> optionsForVersion( final int vers )
    {
        return implOptionsVersionCheck( vers, true, false, true );
    }

    /**
     * In a set of Known Options, get all options added or changed since version {@code vers}, or all options valid
     * at {@code vers}, to implement {@link #optionsNewerThanVersion(int, boolean, boolean)}
     * and {@link #optionsForVersion(int)}.
     * @param vers  Version to compare options against
     * @param getAllForVersion  True to get all valid options ({@code optionsForVersion} mode),
     *     false for newer added or changed options only ({@code optionsNewerThanVersion} modes).
     *     If true and {@code vers} is newer than this version of SOCGameOption, will return
     *     all options known at this version.
     * @param checkValues  If not {@code getAllForVersion}, which mode to run in:
     *     Check options' current values and {@link SOCGameOption#minVersion},
     *     not their {@link SOCGameOption#lastModVersion}?
     *     An option's minimum version can increase based on its value; see {@link SOCGameOption#getMinVersion(Map)}.
     *     <P>
     *     If false, returns list of any game options to send to older server or client {@code vers}.
     *     Ignores any option with {@link SOCGameOption#FLAG_INACTIVE_HIDDEN}.
     *     Adds all options with {@link SOCGameOption#FLAG_ACTIVATED}
     *     having {@link SOCVersionedItem#minVersion minVersion} &lt;= {@code vers},
     *     ignoring their {@link SOCVersionedItem#lastModVersion lastModVersion}
     *     in case activation is the opt's only recent change.
     *     <BR>
     *     If {@code checkValues}, {@code getAllForVersion}, and {@code trimEnums} all false, assumes is
     *     a client-side call and also adds any opts with {@link SOCGameOption#FLAG_3RD_PARTY} to the returned list.
     *     <P>
     *     If true, any returned items are from this Set but too new for client {@code vers}:
     *     Game creation should be rejected.
     *     Does not check {@link SOCGameOption#FLAG_INACTIVE_HIDDEN} in this mode; use
     *     {@link SOCGameOptionSet#adjustOptionsToKnown(SOCGameOptionSet, boolean, SOCFeatureSet)} for that check.
     * @param trimEnums  For enum-type options where minVersion changes based on current value,
     *     should we remove too-new values from the returned option info?
     *     This lets us send only the permitted values to an older client.
     *     Also trims int-type options' max value where needed (example: {@code "PL"}).
     * @return List of the requested {@link SOCGameOption}s, or null if none match the conditions, at {@code vers};
     *     see {@code optionsNewerThanVersion} and {@code optionsForVersion} for return details.
     *     <BR>
     *     <B>Note:</B> If not {@code getAllForVersion}, may include options with
     *     {@link SOCGameOption#minVersion} &gt; {@code vers} if client has asked about them by name.
     * @throws IllegalArgumentException  if {@code getAllForVersion && checkValues}: Cannot combine these modes
     * @since 2.0.00
     */
    private List<SOCGameOption> implOptionsVersionCheck( final int vers, final boolean getAllForVersion,
        final boolean checkValues, final boolean trimEnums )
        throws IllegalArgumentException
    {
        /** collect newer options here, or all options if getAllForVersion */
        List<SOCGameOption> uopt
            = SOCVersionedItem.implItemsVersionCheck( vers, getAllForVersion, checkValues, options );
        // throws IllegalArgumentException if (getAllForVersion && checkValues)

        if (!checkValues)
        {
            if (uopt != null)
            {
                ListIterator<SOCGameOption> li = uopt.listIterator();
                while (li.hasNext())
                {
                    SOCGameOption opt = li.next();
                    if (opt.hasFlag( SOCGameOption.FLAG_INACTIVE_HIDDEN ))
                        li.remove();
                }
            }

            // add any activated ones, even if unchanged since vers
            {
                SOCGameOptionSet actives = optionsWithFlag( SOCGameOption.FLAG_ACTIVATED, vers );
                if (actives != null)
                {
                    if (uopt != null)
                        for (SOCGameOption opt : uopt)
                            actives.remove( opt.key );  // remove if happens to already be in list, to avoid double add
                    else
                        uopt = new ArrayList<>();

                    for (SOCGameOption aopt : actives)
                        uopt.add( aopt );
                }
            }

            // if client-side, also add any with FLAG_3RD_PARTY
            if (!(getAllForVersion || trimEnums))
            {
                for (SOCGameOption opt : options.values())
                {
                    if ((0 == (opt.optFlags & SOCGameOption.FLAG_3RD_PARTY))
                        || (0 != (opt.optFlags & SOCGameOption.FLAG_INACTIVE_HIDDEN)))
                        continue;

                    if (uopt != null)
                    {
                        if (uopt.contains( opt ))
                            continue;
                    }
                    else
                    {
                        uopt = new ArrayList<>();
                    }

                    uopt.add( opt );
                }
            }

            if ((uopt != null) && uopt.isEmpty())
                uopt = null;
        }

        if ((uopt != null) && trimEnums)
        {
            ListIterator<SOCGameOption> li = uopt.listIterator();
            while (li.hasNext())
            {
                boolean changed = false;
                SOCGameOption opt = li.next();

                if ((opt.lastModVersion > vers)   // opt has been modified since vers
                    && (opt.minVersion <= vers))  // vers is new enough to use this opt
                {
                    if (opt.enumVals != null)
                    {
                        // Possibly trim enum values. (OTYPE_ENUM, OTYPE_ENUMBOOL)
                        // OTYPE_* - Add here in comment if enum-valued option type
                        final int ev = SOCGameOption.getMaxEnumValueForVersion( opt.key, vers );
                        if (ev < opt.enumVals.length)
                        {
                            opt = SOCGameOption.trimEnumForVersion( opt, vers );
                            changed = true;
                        }
                    }
                    else if (opt.maxIntValue != opt.minIntValue)
                    {
                        // Possibly trim max int value. (OTYPE_INT, OTYPE_INTBOOL)
                        // OTYPE_* - Add here in comment if int-valued option type
                        final int iv = SOCGameOption.getMaxIntValueForVersion( opt.key, vers );
                        if ((iv != opt.maxIntValue) && (iv != Integer.MAX_VALUE))
                        {
                            opt = new SOCGameOption( opt, iv );
                            changed = true;
                        }
                    }

                    if (changed)
                        li.set( opt );
                }
            }
        }

        return uopt;
    }

    /**
     * NOTE: only called by server
     *
     * Compare this set of options with known-good values, and optionally apply options from
     * the set's scenario (game option <tt>"SC"</tt>) if present.
     *<P>
     * If any values are above/below maximum/minimum, clip to the max/min value given in {@code knownOpts}.
     * If any are unknown or inactive, return a description. Will still check (and clip) the known ones.
     * If any options are default, and unset/blank, and
     * their {@link SOCGameOption#FLAG_DROP_IF_UNUSED} flag is set, remove them from this set.
     * For {@link SOCGameOption#OTYPE_INTBOOL} and {@link SOCGameOption#OTYPE_ENUMBOOL}, both the integer and
     * boolean values are checked against defaults.
     *<P>
     * If <tt>doServerPreadjust</tt> is true, then the server might also change some
     * option values before creating the game, for overall consistency of the set of options.
     * This is a server-side equivalent to the client-side {@link SOCGameOption.ChangeListener}s.
     * For example, if <tt>"PL"</tt> (number of players) > 4, but <tt>"PLB"</tt> (use 6-player board)
     * is not set, <tt>doServerPreadjust</tt> wil set the <tt>"PLB"</tt> option.
     * {@code doServerPreadjust} will also remove any game-internal options the client has sent.
     *<P>
     * Before any other adjustments when <tt>doServerPreadjust</tt>, will check for
     * the game scenario option <tt>"SC"</tt>. If that option is set, call
     * {@link soc.server.SOCServerScenario#getScenario(String)}; the scenario name must be known.
     * Then, add that scenario's {@link SOCScenario#scOpts .scOpts} into this set.
     * Scenario option values always overwrite those already in the set, except for <tt>"VP"</tt>
     * where current value (if any) is kept.
     *<P>
     * Client-side gameopt code also assumes all scenarios use the sea board,
     * and sets game option <tt>"SBL"</tt> when a scenario is chosen by the user.
     *<P>
     * Before v2.4.50 this method was {@code SOCGameOption.adjustOptionsToKnown(newOpts, knownOpts, boolean)}.
     *
     * @param knownOpts Set of known {@link SOCGameOption}s to check against; not null.
     *     Caller can use {@link soc.server.SOCServerGameOptionSet#getAllKnownOptions()} if they don't already have such a set.
     * @param doServerPreadjust  If true, we're calling from the server before creating a game;
     *     pre-adjust any values for consistency.
     *     This is a server-side equivalent to the client-side {@link SOCGameOption.ChangeListener}s.
     *     (Added in 1.1.13)
     * @param limitedCliFeats For {@code doServerPreadjust}, client's set of features if limited compared to
     *     the standard client; null if client doesn't have limited feats.
     *     See {@link SOCClientData#hasLimitedFeats} for details.
     * @return <tt>null</tt> if all are known; or, a human-readable problem description if:
     *     <UL>
     *       <LI> any option in this set not a Known Option
     *            or is inactive (has {@link SOCGameOption#FLAG_INACTIVE_HIDDEN})
     *       <LI> or an opt's type differs from that in knownOpts
     *       <LI> or an opt's {@link SOCGameOption#lastModVersion} differs from in knownOpts
     *       <LI> or an opt requires a {@link SOCGameOption#getClientFeature()} which the client doesn't have
     *            (checked only if {@code limitedCliFeats} != null and {@code doServerPreadjust})
     *       <LI> set has option {@code "SC"} but its scenario keyname isn't known
     *            by {@link soc.server.SOCServerScenario#getScenario(String)}
     *     </UL>
     * @throws IllegalArgumentException if {@code knownOpts} is null
     * @since 1.1.07
     */
    public StringBuilder adjustOptionsToKnown( SOCGameOptionSet knownOpts, boolean doServerPreadjust,
        SOCFeatureSet limitedCliFeats, Map<String,SOCScenario> scenarioMap )
        throws IllegalArgumentException
    {
        if (knownOpts == null)
            throw new IllegalArgumentException( "null" );

        String unknownScenario = null;

        if (doServerPreadjust)
        {
            // Remove any game-internal options, before adding scenario opts
            {
                Iterator<String> ki = options.keySet().iterator();  // keySet lets us remove without disrupting iterator
                while (ki.hasNext())
                {
                    SOCGameOption op = options.get( ki.next() );
                    if (0 != (op.optFlags & SOCGameOption.FLAG_INTERNAL_GAME_PROPERTY))
                        ki.remove();
                }
            }

            // If has "VP" but boolean part is false, use server default instead
            SOCGameOption opt = options.get( "VP" );
            if ((opt != null) && !opt.getBoolValue())
                options.remove( "VP" );

            // Apply scenario options, if any
            opt = options.get( "SC" );
            if (opt != null)
            {
                final String scKey = opt.getStringValue();
                if (scKey.length() > 0)
                {
                    SOCScenario sc = scenarioMap.get( scKey );
                    if (sc == null)
                    {
                        unknownScenario = scKey;
                    }
                    else
                    {
                        // include this scenario's opts,
                        // overwriting any values for those
                        // opts if already in newOpts, except
                        // keep VP if specified.
                        opt = options.get( "VP" );

                        final Map<String, SOCGameOption> scOpts = SOCGameOption.parseOptionsToMap( sc.scOpts, knownOpts );
                        if (scOpts.containsKey( "VP" ) && (opt != null))
                            scOpts.remove( "VP" );

                        options.putAll( scOpts );
                    }
                }

                // Client-side gameopt code also assumes all scenarios use
                // the sea board, and sets game option "SBL" when a scenario
                // is chosen by the user.
            }

            // NEW_OPTION: If you created a ChangeListener, you should probably add similar code
            //    here. Set or change options if it makes sense; if a user has deliberately
            //    set a boolean option, think carefully before un-setting it and surprising them.

            // Set PLB if PL>4 or PLP
            opt = options.get( "PL" );
            SOCGameOption optPLP = options.get( "PLP" );
            if (((opt != null) && (opt.getIntValue() > 4))
                || ((optPLP != null) && optPLP.getBoolValue()))
            {
                setBoolOption( "PLB", knownOpts );
            }
        }  // if(doServerPreadjust)

        // OTYPE_* - adj javadoc above (re dropIfUnused) if a string-type or bool-type is added.

        StringBuilder optProblems = new StringBuilder();

        boolean allKnown;

        if (unknownScenario != null)
        {
            allKnown = false;
            optProblems.append( "SC: unknown scenario " );
            optProblems.append( unknownScenario );
            optProblems.append( ". " );
        }
        else
        {
            allKnown = true;  // might be set false in loop below
        }

        // use Iterator in loop, so we can remove from the hash if needed
        for (Iterator<Map.Entry<String, SOCGameOption>> ikv = options.entrySet().iterator();
             ikv.hasNext(); )
        {
            Map.Entry<String, SOCGameOption> okv = ikv.next();

            SOCGameOption op;
            try
            {
                op = okv.getValue();
            }
            catch( ClassCastException ce )
            {
                throw new IllegalArgumentException( "wrong class, expected gameoption" );
            }

            SOCGameOption knownOp = knownOpts.get( op.key );
            if (knownOp == null)
            {
                allKnown = false;
                optProblems.append( op.key );
                optProblems.append( ": unknown. " );
            }
            else if (knownOp.hasFlag( SOCGameOption.FLAG_INACTIVE_HIDDEN ))
            {
                allKnown = false;
                optProblems.append( op.key );
                optProblems.append( ": inactive. " );
            }
            else if (knownOp.optType != op.optType)
            {
                allKnown = false;
                optProblems.append( op.key );
                optProblems.append( ": optType mismatch (" );
                optProblems.append( knownOp.optType );
                optProblems.append( " != " );
                optProblems.append( op.optType );
                optProblems.append( "). " );
            }
            else
            {
                // Clip int values, check default values, check dropIfUnused

                if (knownOp.lastModVersion != op.lastModVersion)
                {
                    allKnown = false;
                    optProblems.append( op.key );
                    optProblems.append( ": lastModVersion mismatch (" );
                    optProblems.append( knownOp.lastModVersion );
                    optProblems.append( " != " );
                    optProblems.append( op.lastModVersion );
                    optProblems.append( "). " );
                }

                switch (op.optType)  // OTYPE_*
                {
                case SOCGameOption.OTYPE_INT:
                case SOCGameOption.OTYPE_INTBOOL:
                case SOCGameOption.OTYPE_ENUM:
                case SOCGameOption.OTYPE_ENUMBOOL:
                {
                    int iv = op.getIntValue();
                    if (iv < knownOp.minIntValue)
                    {
                        iv = knownOp.minIntValue;
                        op.setIntValue( iv );
                    }
                    else if (iv > knownOp.maxIntValue)
                    {
                        iv = knownOp.maxIntValue;
                        op.setIntValue( iv );
                    }

                    if (knownOp.hasFlag( FLAG_DROP_IF_UNUSED )
                        && (iv == knownOp.defaultIntValue))
                    {
                        // ignore boolValue unless also boolean-type: OTYPE_INTBOOL and OTYPE_ENUMBOOL.
                        if ((op.optType == SOCGameOption.OTYPE_INT) || (op.optType == SOCGameOption.OTYPE_ENUM)
                            || !op.getBoolValue())
                            ikv.remove();
                    }
                }
                break;

                case SOCGameOption.OTYPE_BOOL:
                    if (knownOp.hasFlag( FLAG_DROP_IF_UNUSED ) && !op.getBoolValue())
                        ikv.remove();
                    break;

                case SOCGameOption.OTYPE_STR:
                case SOCGameOption.OTYPE_STRHIDE:
                    if (knownOp.hasFlag( FLAG_DROP_IF_UNUSED ))
                    {
                        String sval = op.getStringValue();
                        if ((sval == null) || (sval.length() == 0))
                            ikv.remove();
                    }
                    break;

                // no default: all types should be handled above.

                }
            }
        }

        if (doServerPreadjust && allKnown && (limitedCliFeats != null))
        {
            // See also SOCServerMessageHandler.handleGAMEOPTIONGETINFOS which has
            // very similar code for limited client feats.

            final Map<String, SOCGameOption> unsupportedOpts = optionsNotSupported( limitedCliFeats );

            if (unsupportedOpts != null)
            {
                allKnown = false;
                for (String okey : unsupportedOpts.keySet())
                {
                    if (optProblems.length() > 0)
                        optProblems.append( ", " );
                    optProblems.append( okey );
                }
                optProblems.append( ": requires missing feature(s). " );
            }
            else
            {
                final Map<String, SOCGameOption> trimmedOpts = optionsTrimmedForSupport( limitedCliFeats );

                if (trimmedOpts != null)
                    for (SOCGameOption opt : trimmedOpts.values())
                        options.put( opt.key, opt );
            }
        }

        if (allKnown)
            return null;
        else
            return optProblems;
    }

    /**
     * In a set of options or Known Options, do any require client features
     * not supported by a limited client's {@link SOCFeatureSet}?
     * Checks each option having a {@link SOCGameOption#getClientFeature()}.
     *<P>
     * Doesn't check integer value of features like {@code sc} ({@link SOCFeatureSet#getValue(String, int)}):
     * Use {@link SOCGame#checkClientFeatures(SOCFeatureSet, boolean)} for that.
     *
     * @param cliFeats  Client's limited subset of optional features,
     *     from {@link SOCClientData#feats}, or {@code null} or empty set if no features
     * @return Map of known options not supported by {@code cliFeats},
     *     or {@code null} if all known options are supported.
     *     Each map key is its option value's {@link SOCVersionedItem#key key}.
     * @see #optionsTrimmedForSupport(SOCFeatureSet)
     * @since 2.4.00
     */
    public Map<String, SOCGameOption> optionsNotSupported( final SOCFeatureSet cliFeats )
    {
        Map<String, SOCGameOption> ret = null;

        for (SOCGameOption opt : options.values())
        {
            final String cliFeat = opt.getClientFeature();
            if (cliFeat == null)
                continue;
            if ((cliFeats != null) && cliFeats.isActive( cliFeat ))
                continue;

            if (ret == null)
                ret = new HashMap<>();
            ret.put( opt.key, opt );
        }

        return ret;
    }

    /**
     * In a set of options or Known Options, do any require changes for a limited client's {@link SOCFeatureSet}?
     * For example, clients without {@link SOCFeatureSet#CLIENT_6_PLAYERS} limit "max players" to 4.
     *<P>
     * Assumes client is new enough that its version wouldn't also cause trimming of those same options' values
     * by {@link #optionsNewerThanVersion(int, boolean, boolean)} or {@link #optionsForVersion(int)}.
     *
     * @param cliFeats  Client's limited subset of optional features,
     *     from {@link SOCClientData#feats}, or {@code null} or empty set if no features
     * @return Map of trimmed known options, or {@code null} if no trimming was needed.
     *     Each map key is its option value's {@link SOCVersionedItem#key key}.
     * @see #optionsNotSupported(SOCFeatureSet)
     * @see SOCGameOption#getMaxIntValueForVersion(String, int)
     * @since 2.4.00
     */
    public Map<String, SOCGameOption> optionsTrimmedForSupport( final SOCFeatureSet cliFeats )
    {
        if ((cliFeats != null) && cliFeats.isActive( SOCFeatureSet.CLIENT_6_PLAYERS ))
            return null;

        SOCGameOption pl = getKnownOption( "PL", false );
        if (pl == null)
            return null;  // shouldn't happen, PL is a known option

        Map<String, SOCGameOption> ret = new HashMap<>();
        ret.put( "PL", new SOCGameOption( pl, SOCGame.MAXPLAYERS_STANDARD ) );
        return ret;
    }

    /**
     * Find all opts in this set having the specified flag(s) and optional minimum version,
     * or all Known Options when called on a server or client's set of Known Options.
     *<P>
     * Some uses:
     *<UL>
     * <LI> {@link SOCGameOption#FLAG_3RD_PARTY}:
     *   Find any Third-party Options defined at client, to ask server if it knows them too.
     *   Client calls this as part of connect to server, ignoring minVersion so all are asked about.
     * <LI> {@link SOCGameOption#FLAG_ACTIVATED}:
     *   Find any Activated Options compatible with client version.
     *   Server calls this as part of client connect, with {@code minVers} = client version.
     *</UL>
     * Ignores any option with {@link SOCGameOption#FLAG_INACTIVE_HIDDEN} unless that's part of {@code flagMask}.
     *<P>
     * If calling at server and the connecting client has limited features, assumes has already
     * called {@link #optionsNotSupported(SOCFeatureSet)} and {@link #optionsTrimmedForSupport(SOCFeatureSet)}.
     * So this method filters only by minVersion, not by feature requirement or any other field.
     *
     * @param flagMask  Flag(s) to check for; {@link SOCGameOption#hasFlag(int)} return value is the filter
     * @param minVers  Minimum compatible version to look for, same format as {@link Version#versionNumber()},
     *     or 0 to ignore {@link SOCVersionedItem#minVersion opt.minVersion}
     * @return Map of found options compatible with {@code minVers}, or {@code null} if none.
     *     Each map key is its option value's {@link SOCVersionedItem#key key}.
     * @see #activate(String)
     */
    public SOCGameOptionSet optionsWithFlag( final int flagMask, final int minVers )
    {
        Map<String, SOCGameOption> ret = null;
        final boolean ignoreInactives = (0 == (flagMask & SOCGameOption.FLAG_INACTIVE_HIDDEN));

        for (final SOCGameOption opt : options.values())
        {
            if ((minVers != 0) && (opt.minVersion > minVers))
                continue;

            if (ignoreInactives && (0 != (opt.optFlags & SOCGameOption.FLAG_INACTIVE_HIDDEN)))
                continue;

            if (opt.hasFlag( flagMask ))
            {
                if (ret == null)
                    ret = new HashMap<>();
                ret.put( opt.key, opt );
            }
        }

        return (ret != null) ? new SOCGameOptionSet( ret ) : null;
    }

    /**
     * Human-readable contents of the Set: Returns its game options {@link Map#toString()}.
     */
    @Override
    public String toString()
    {
        return options.toString();
    }

}
