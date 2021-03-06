/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * This file Copyright (C) 2008,2010-2012,2014 Jeremy D Monin <jeremy@nand.net>
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
 * @apiNote UNUSED
 *
 * Template for per-game message types with 1 string parameter.
 * Your class javadoc should explain the meaning of param1,
 * so that you won't need to write a getter for it.
 *<P>
 * You will have to write parseDataStr, because of its return
 * type and because it's static.
 *<P>
 * Sample implementation:
 *<code><pre>
 *   public static SOCSitDown parseDataStr(final String s)
 *   {
 *       String ga; // the game name
 *       String pna; // the player name
 *
 *       StringTokenizer st = new StringTokenizer(s, sep2);
 *
 *       try
 *       {
 *           ga = st.nextToken();
 *           pna = st.nextToken();
 *       }
 *       catch (Exception e)
 *       {
 *           return null;
 *       }
 *
 *        return new SOCSitDown(ga, pna);
 *   }
 *</pre></code>
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.00
 */
@Deprecated
public abstract class SOCMessageTemplate1s extends SOCMessageForGame
{
    private static final long serialVersionUID = 2000L;

    /**
     * Single string parameter.
     */
    protected String p1;

    /**
     * Create a new message.
     *
     * @param messageType  Message type ID
     * @param gameName  Name of game this message is for
     * @param p   the string parameter
     */
    protected SOCMessageTemplate1s( int messageType, String gameName, String p)
    {
        super( messageType, gameName );
        p1 = p;
    }

    /**
     * @return the single parameter
     */
    public String getParam()
    {
        return p1;
    }

    /**
     * MESSAGETYPE sep game sep2 param
     *
     * @return the command String
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + p1 );
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return getClass().getSimpleName() + ":game=" + getGameName() + "|param=" + p1;
    }
}
