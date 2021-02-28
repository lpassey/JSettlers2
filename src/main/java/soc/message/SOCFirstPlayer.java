/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2014,2017,2020 Jeremy D Monin <jeremy@nand.net>
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
 * This message says who the first player number is.
 *<P>
 * In games where all clients are v2.0.00 or newer, send {@link SOCGameElements.GEType#FIRST_PLAYER}
 * instead: Check clients' version against {@link SOCGameElements#MIN_VERSION}.
 *
 * @author Robert S. Thomas
 */
public class SOCFirstPlayer extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The seat number
     */
    private int playerNumber;

    /**
     * Create a FirstPlayer message.
     *
     * @param gameName  the name of the game
     * @param pn  the seat number
     */
    public SOCFirstPlayer(String gameName, int pn)
    {
        super( FIRSTPLAYER, gameName );
        playerNumber = pn;
    }

    /**
     * @return the seat number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * FIRSTPLAYER sep game sep2 playerNumber
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd(sep2 + playerNumber );
    }

    /**
     * Parse the command String into a FIRSTPLAYER message.
     *
     * @param s   the String to parse
     * @return    a FIRSTPLAYER message, or null if the data is garbled
     */
    public static SOCFirstPlayer parseDataStr(String s)
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
            return new SOCFirstPlayer( parsed.get(0), Integer.parseInt(parsed.get( 1 )));
        else
            return null;
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCFirstPlayer:game=" + getGameName() + "|playerNumber=" + playerNumber;
    }
}
