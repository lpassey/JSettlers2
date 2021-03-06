/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2014,2017-2018,2020 Jeremy D Monin <jeremy@nand.net>
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
 * This message has the total resource count for a player.
 * Includes all resources of known and unknown types, from
 * player's hand's {@link soc.game.SOCResourceSet#getTotal()}.
 *<P>
 * In games where all clients are v2.0.00 or newer, send {@link SOCPlayerElement.PEType#RESOURCE_COUNT} instead:
 * Check clients' version against {@link SOCPlayerElement#VERSION_FOR_CARD_ELEMENTS}.
 * For dice rolls, check against {@link SOCDiceResultResources#VERSION_FOR_DICERESULTRESOURCES}
 * and send {@link SOCDiceResultResources} instead.
 *<P>
 * v2.0.00 and newer clients still accept this message because of older servers and games which include older clients.
 *
 * @author Robert S. Thomas
 */
public class SOCResourceCount extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The seat number
     */
    private int playerNumber;

    /**
     * The resource count
     */
    private int count;

    /**
     * Create a ResourceCount message.
     *
     * @param gameName  the name of the game
     * @param pn  the seat number
     * @param rc  the resource count
     */
    public SOCResourceCount(String gameName, int pn, int rc)
    {
        super( RESOURCECOUNT, gameName );
        playerNumber = pn;
        count = rc;
    }

    /**
     * @return the seat number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return the recource count
     */
    public int getCount()
    {
        return count;
    }

    /**
     * RESOURCECOUNT sep game sep2 playerNumber sep2 count
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + playerNumber + sep2 + count );
    }

    /**
     * Parse the command String into a ResourceCount message
     *
     * @param s   the String to parse
     * @return    a ResourceCount message, or null if the data is garbled
     */
    public static SOCResourceCount parseDataStr(String s)
    {
        ArrayList<String> parsed = parseDataStr( s, 3 );
        if (null != parsed)
            return new SOCResourceCount( parsed.get( 0 ), Integer.parseInt( parsed.get( 1 )),
                Integer.parseInt( parsed.get( 2 )));
        return null;
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCResourceCount:game=" + getGameName() + "|playerNumber=" + playerNumber + "|count=" + count;
    }
}
