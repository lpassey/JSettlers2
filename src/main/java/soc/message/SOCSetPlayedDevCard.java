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

import java.util.StringTokenizer;


/**
 * This message sets the flag which says if a player has played a development card this turn.
 *<P>
 * In games where all clients are v2.0.00 or newer, send {@link SOCPlayerElement.PEType#PLAYED_DEV_CARD_FLAG} instead:
 * Check clients' version against {@link SOCPlayerElement#VERSION_FOR_CARD_ELEMENTS}.
 *
 * @author Robert S. Thomas
 */
@Deprecated
public class SOCSetPlayedDevCard extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The player number
     */
    private int playerNumber;

    /**
     * the value of the playedDevCard flag
     */
    private boolean playedDevCard;

    /**
     * Create a SetPlayedDevCard message.
     *
     * @param gameName  the name of the game
     * @param pn  the seat number
     * @param pd  the value of the playedDevCard flag
     */
    public SOCSetPlayedDevCard( String gameName, int pn, boolean pd )
    {
        super( SETPLAYEDDEVCARD, gameName );
        playerNumber = pn;
        playedDevCard = pd;
    }

    /**
     * @return the seat number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return the value of the playedDevCard flag
     */
    public boolean hasPlayedDevCard()
    {
        return playedDevCard;
    }

    /**
     * SETPLAYEDDEVCARD sep game sep2 playerNumber sep2 playedDevCard
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + playerNumber + sep2 + playedDevCard );
    }

    /**
     * Parse the command String into a SETPLAYEDDEVCARD message.
     *
     * @param s   the String to parse
     * @return    a SETPLAYEDDEVCARD message, or null if the data is garbled
     */
    public static SOCSetPlayedDevCard parseDataStr(String s)
    {
        String gameName; // the game name
        int pn; // the seat number
        boolean pd; // the value of the playedDevCard flag

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            gameName = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            pd = Boolean.valueOf( st.nextToken() );
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCSetPlayedDevCard(gameName, pn, pd);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCSetPlayedDevCard:game=" + getGameName() + "|playerNumber=" + playerNumber
            + "|playedDevCard=" + playedDevCard;
    }
}
