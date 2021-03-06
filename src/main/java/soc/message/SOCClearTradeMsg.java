/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010-2011,2014,2017,2019-2020 Jeremy D Monin <jeremy@nand.net>
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

import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * This message means that the server wants any trade messages/responses cleared in the client UI.
 *<P>
 * Note: When a player makes a new trade offer, the server announces that with a {@link SOCMakeOffer} message
 * followed immediately by a {@code SOCClearTradeMsg} to clear responses from any previous offer.
 *<P>
 * Version 1.1.12 and newer: If <tt>playerNumber</tt> is -1, all players are clearing trade messages.
 *
 * @author Robert S. Thomas
 */
public class SOCClearTradeMsg extends SOCMessageForGame
{
    private static final long serialVersionUID = 1112L;  // last structural change v1.1.12

    /**
     * Minimum version (1.1.12) which supports playerNumber -1 for clear all.
     * @since 1.1.12
     */
    public static final int VERSION_FOR_CLEAR_ALL = 1112;

    /**
     * The seat number, or -1 to clear all seats
     */
    private int playerNumber;

    /**
     * Create a ClearTradeMsg message.
     *
     * @param gameName  the name of the game
     * @param seatNumber  the seat number
     */
    public SOCClearTradeMsg(String gameName, int seatNumber)
    {
        super( CLEARTRADEMSG, gameName );
        playerNumber = seatNumber;
    }

    /**
     * @return the seat number, or -1 to clear all seats
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * CLEARTRADEMSG sep game sep2 playerNumber
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + playerNumber );
    }

    /**
     * Parse the command String into a CLEARTRADEMSG message
     *
     * @param s   the String to parse
     * @return    a CLEARTRADEMSG message, or null if the data is garbled
     */
    public static SOCClearTradeMsg parseDataStr(String s)
    {
        ArrayList<String> parsed = parseDataStr( s, 2 );
/*
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
*/
        if (null != parsed)
            return new SOCClearTradeMsg( parsed.get(0), Integer.parseInt(parsed.get( 1 )));
        else
            return null;
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCClearTradeMsg:game=" + getGameName() + "|playerNumber=" + playerNumber;
    }
}
