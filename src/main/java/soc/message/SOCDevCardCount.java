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

import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * This message says how many development cards are available in the deck to be bought and drawn.
 *<P>
 * In games where all clients are v2.0.00 or newer, send {@link SOCGameElements.GEType#DEV_CARD_COUNT}
 * instead: Check clients' version against {@link SOCGameElements#MIN_VERSION}.
 *<P>
 * For message sequence, see {@link SOCGameElements.GEType#DEV_CARD_COUNT} javadoc:
 * This message is sent to older clients whenever {@code DEV_CARD_COUNT} would be sent.
 *
 * @author Robert S. Thomas
 */
public class SOCDevCardCount extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The number of dev cards
     */
    private int numDevCards;

    /**
     * Create a DevCardCount message.
     *
     * @param gameName  the name of the game
     * @param nd  the number of dev cards
     */
    public SOCDevCardCount(String gameName, int nd)
    {
        super( DEVCARDCOUNT, gameName );
        numDevCards = nd;
    }

    /**
     * @return the number of dev cards
     */
    public int getNumDevCards()
    {
        return numDevCards;
    }

    /**
     * DEVCARDCOUNT sep game sep2 numDevCards
     *
     * @return the command string
     */
    public String toCmd()
    {
        return super.toCmd( sep2 + numDevCards );
    }

    /**
     * Parse the command String into a DevCardCount message
     *
     * @param s   the String to parse
     * @return    a DevCardCount message, or null if the data is garbled
     */
    public static SOCDevCardCount parseDataStr(String s)
    {
        ArrayList<String> parsed = parseDataStr( s, 2 );
/*
        String ga; // the game name
        int nd; // the number of dev cards

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            nd = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }
*/
        if (null != parsed)
            return new SOCDevCardCount( parsed.get( 0 ), Integer.parseInt( parsed.get( 1 )));
        else
            return null;
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCDevCardCount:game=" + getGameName() + "|numDevCards=" + numDevCards;
    }

}
