/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2014,2017 Jeremy D Monin <jeremy@nand.net>
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
 * This message means that a player wants to end the turn
 *
 * @author Robert S. Thomas
 */
public class SOCEndTurn extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * Create a EndTurn message.
     *
     * @param gameName  the name of the game
     */
    public SOCEndTurn(String gameName)
    {
        super( ENDTURN, gameName );
     }

    /**
     *  * Parse the command String into a EndTurn message
     *
     * @param s   the String to parse
     * @return    a EndTurn message, or null if the data is garbled
     */
    public static SOCEndTurn parseDataStr(String s)
    {
        return new SOCEndTurn(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCEndTurn:game=" + getGameName();
    }
}
