/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2014,2017,2019-2020 Jeremy D Monin <jeremy@nand.net>
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

import java.util.StringTokenizer;


/**
 * This message from server says which player has Longest Road/Longest Route.
 * Sent during joinGame and when changes occur during normal gameplay.
 *<P>
 * In versions older than v2.4.00, Longest Route at client was updated during gameplay
 * by examining game state, not by messages from server:
 * See {@link SOCPutPiece}({@link soc.game.SOCPlayingPiece#ROAD ROAD}).
 *<P>
 * In games where all clients are v2.0.00 or newer, send {@link SOCGameElements.GEType#LONGEST_ROAD_PLAYER}
 * instead: Check clients' version against {@link SOCGameElements#MIN_VERSION}.
 *
 * @author Robert S. Thomas
 */
public class SOCLongestRoad extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The number of the player with longest road
     */
    private int playerNumber;

    /**
     * Create a LongestRoad message.
     *
     * @param ga  the name of the game
     * @param pn  the seat number
     */
    public SOCLongestRoad(String ga, int pn)
    {
        super( LONGESTROAD, ga );
        playerNumber = pn;
    }

    /**
     * @return the number of the player with longest road
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * LONGESTROAD sep game sep2 playerNumber
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd() + sep2 + playerNumber;
    }

    /**
     * Parse the command String into a LONGESTROAD message.
     *
     * @param s   the String to parse
     * @return    a LONGESTROAD message, or null if the data is garbled
     */
    public static SOCLongestRoad parseDataStr(String s)
    {
        String ga; // the game name
        int pn; // the seat number

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCLongestRoad(ga, pn);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCLongestRoad:game=" + getGame() + "|playerNumber=" + playerNumber;
    }
}
