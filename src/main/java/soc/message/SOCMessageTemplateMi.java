/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * This file Copyright (C) 2008-2012,2015-2017 Jeremy D Monin <jeremy@nand.net>
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
package soc.message;


/**
 * Template for message types with variable number of integer parameters.
 * You will have to write parseDataStr, because of its subclass return
 * type and because it's static.
 *<P>
 * Sample implementation:
 *<pre><code>
 *   // format of s: POTENTIALSETTLEMENTS sep game sep2 settlecoord {sep2 settlecoord}*...
 *   // Must have at least game + 1 settlement param.
 *   public static SOCPotentialSettlements parseDataStr(String[] s)
 *   {
 *       String ga; // the game name
 *       int[] sett; // the settlements
 *
 *       if ((s == null) || (s.length < 2))
 *           return null;  // must have at least game + 1 settlement param
 *
 *       ga = s[0];
 *       sett = new int[s.length - 1];
 *       try
 *       {
 *           for (int i = 1; i < s.length; ++i)
 *               sett[i-1] = Integer.parseInt(s[i]);
 *       }
 *       catch (Exception e)
 *       {
 *           return null;
 *       }
 *
 *       return new SOCPotentialSettlements(ga, sett);
 *   }
 *</code></pre>
 *<P>
 * For notes on the section you must add to {@link SOCMessage#toMsg(String)},
 * see {@link SOCMessageMulti}.
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.00
 * @see SOCMessageTemplateMs
 */
public abstract class SOCMessageTemplateMi extends SOCMessageForGame
{
    private static final long serialVersionUID = 2000L;

    /**
     * Array of int parameters, or null if none.
     *<P>
     * Although {@link SOCMessageTemplateMs} uses a List in v2.0.00 and newer,
     * for now this class still uses an array for compact representation.
     */
    protected int[] pa;

    /**
     * Create a new multi-message with integer parameters.
     *
     * @param id  Message type ID
     * @param ga  Name of game this message is for, or null if none
     * @param parr   Parameters, or null if none
     */
    protected SOCMessageTemplateMi(int id, String ga, int[] parr)
    {
        super( id, ga );
        pa = parr;
    }

    /**
     * @return the parameters, or null if none
     */
    public int[] getParams()
    {
        return pa;
    }

    /**
     * MESSAGETYPE [sep game] sep param1 sep param2 sep ...
     *
     * @return the command String
     */
    @Override
    public String toCmd()
    {
        StringBuilder sb = new StringBuilder( super.toCmd() );
        if (pa != null)
        {
            for (int value : pa )
            {
                sb.append( sep );
                sb.append( value );
            }
        }
        return sb.toString();
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        if (getGame() != null)
        {
            sb.append (":game=");
            sb.append (getGame());
        }
        if (pa != null)
        {
            for (int value : pa)
            {
                sb.append( "|p=" );
                sb.append( value );
            }
        }
        return sb.toString();
    }
}
