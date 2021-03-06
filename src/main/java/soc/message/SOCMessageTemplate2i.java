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
 * Template for per-game message types with 2 integer parameters.
 * Your class javadoc should explain the meaning of param1 and param2,
 * so that you won't need to write getters for those.
 *<P>
 * You will have to write parseDataStr, because of its return
 * type and because it's static.
 *<P>
 * Sample implementation:
 *<code><pre>
 *   // format of s: LONGESTROAD sep game sep2 seatnumber sep2 coordinates
 *   public static SOCLongestRoad parseDataStr(final String s)
 *   {
 *       String ga; // the game name
 *       int pn; // the seat number
 *       int co; // coordinates
 *
 *       StringTokenizer st = new StringTokenizer(s, sep2);
 *
 *       try
 *       {
 *           ga = st.nextToken();
 *           pn = Integer.parseInt(st.nextToken());
 *           co = Integer.parseInt(st.nextToken());
 *       }
 *       catch (Exception e)
 *       {
 *           return null;
 *       }
 *
 *        return new SOCMoveRobber(ga, pn, co);
 *   }
 *</pre></code>
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.00
 */
public abstract class SOCMessageTemplate2i extends SOCMessageForGame
{
    private static final long serialVersionUID = 2000L;

    /**
     * First integer parameter.
     */
    protected int p1;

    /**
     * Second integer parameter.
     */
    protected int p2;

    /**
     * Create a new message.
     *
     * @param messageType  Message type ID
     * @param gameName  Name of game this message is for
     * @param p1  Parameter 1
     * @param p2  Parameter 2
     */
    protected SOCMessageTemplate2i(int messageType, String gameName, int p1, int p2)
    {
        super( messageType, gameName );
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * @return the first parameter
     */
    public int getParam1()
    {
        return p1;
    }

    /**
     * @return the second parameter
     */
    public int getParam2()
    {
        return p2;
    }

    /**
     * MESSAGETYPE sep game sep2 param1 sep2 param2
     *
     * @return the command String
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + p1 + sep2 + p2 );
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return getClass().getSimpleName() + ":game=" + getGameName()
            + "|param1=" + p1 + "|param2=" + p2;
    }
}
