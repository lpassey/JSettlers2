/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2012 Jeremy D Monin <jeremy@nand.net>
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
 * Template for per-game message types with 3 integer parameters.
 * Your class javadoc should explain the meaning of param1, param2, and param3,
 * so that you won't need to write getters for those.
 *<P>
 * You will have to write parseDataStr, because of its return
 * type and because it's static.
 *<P>
 * Sample implementation:
 *<code><pre>
 *   // format of s: game sep2 hexcoord sep2 hextype sep2 dicenum
 *   public static SOCRevealFogHex parseDataStr(final String s)
 *   {
 *       String ga; // the game name
 *       int hc; // the hex coordinate
 *       int ht; // hex type
 *       int dn; // dice number
 *
 *       StringTokenizer st = new StringTokenizer(s, sep2);
 *
 *       try
 *       {
 *           ga = st.nextToken();
 *           hc = Integer.parseInt(st.nextToken());
 *           ht = Integer.parseInt(st.nextToken());
 *           dn = Integer.parseInt(st.nextToken());
 *       }
 *       catch (Exception e)
 *       {
 *           return null;
 *       }
 *
 *        return new SOCRevealFogHex(ga, hc, ht, dn);
 *   }
 *</pre></code>
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 2.0.00
 */
public abstract class SOCMessageTemplate3i extends SOCMessageForGame
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
     * Third integer parameter.
     */
    protected int p3;

    /**
     * Create a new message.
     *
     * @param messageType  Message type ID
     * @param gameName  Name of game this message is for
     * @param param1  Parameter 1
     * @param param2  Parameter 2
     * @param param3  Parameter 3
     */
    protected SOCMessageTemplate3i(final int messageType, final String gameName, final int param1, final int param2, final int param3)
    {
        super( messageType, gameName );
        p1 = param1;
        p2 = param2;
        p3 = param3;
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
     * @return the third parameter
     */
    public int getParam3()
    {
        return p3;
    }

    /**
     * MESSAGETYPE sep game sep2 param1 sep2 param2 sep2 param3
     *
     * @return the command String
     */
    @Override
    public String toCmd()
    {
        return super.toCmd(sep2 + p1 + sep2 + p2 + sep2 + p3);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return getClass().getSimpleName() + ":game=" + getGameName()
            + "|param1=" + p1 + "|param2=" + p2 + "|param3=" + p3;
    }
}
