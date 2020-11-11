/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010-2011,2013-2014,2017-2018,2020 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2017-2018 Strategic Conversation (STAC Project) https://www.irit.fr/STAC/
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

import soc.game.SOCGame;  // for javadocs only


/**
 * This message means:
 * (from client to server) Client player wants to move the robber or pirate ship;
 * or (from server to all players) a player has moved the robber or pirate ship.
 *<P>
 * When sent from current player's client, is in response to server's
 * {@link SOCGameState}({@link SOCGame#PLACING_ROBBER PLACING_ROBBER}
 * or {@link SOCGame#PLACING_PIRATE PLACING_PIRATE}).
 *<P>
 * When sent from server, this message will be followed by other messages
 * about gaining/losing resources: {@link SOCReportRobbery} or {@link SOCPlayerElement}.
 * So for this message, the client should only call {@link soc.game.SOCBoard#setRobberHex(int, boolean)}
 * and not {@link soc.game.SOCGame#moveRobber(int, int)}.
 *<P>
 * Once the robber is placed on the board, it cannot be taken off the board.
 * The pirate can be taken off by sending {@code coordinate = 0}.
 *<P>
 * This message uses positive coordinates when moving the robber, and negative
 * when moving the pirate.  Moving the pirate to hex 0x0104 is done with a
 * SOCMoveRobber(-0x0104) message, which would cause the client to call
 * {@link soc.game.SOCBoardLarge#setPirateHex(int, boolean) board.setPirateHex(0x0104, ...)}.
 *
 * @author Robert S Thomas
 * @see SOCMovePiece
 */
public class SOCMoveRobber extends SOCMessageTemplate2i
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11


    /**
     * the seat number of the player moving the robber. Is {@code p1}
     */
//    private int playerNumber;

    /**
     * the hex coordinates of the piece (positive for robber, negative for pirate). Is {@code p2}
     */
//    private int coordinates;

    /**
     * create a MoveRobber message
     *
     * @param gameName  name of the game
     * @param pn  player seat number
     * @param co  hex coordinates: positive for robber, negative or 0 for pirate
     */
    public SOCMoveRobber(String gameName, int pn, int co)
    {
        super( MOVEROBBER, gameName, pn, co );
    }

    /**
     * @return the player number
     */
    public int getPlayerNumber()
    {
        return p1;
    }

    /**
     * Get the robber or pirate's new location.
     * @return the hex coordinates: positive for robber, negative or 0 for pirate
     */
    public int getCoordinates()
    {
        return p2;
    }

    /**
     * parse the command string into a MoveRobber message
     *
     * @param s   the String to parse
     * @return    a SOCMoveRobber message, or null if the data is garbled
     */
    public static SOCMoveRobber parseDataStr( String s )
    {
        String na; // name of the game
        int pn; // player number
        int co; // coordinates

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            na = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            co = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCMoveRobber(na, pn, co);
    }

    /**
     * Strip out the parameter/attribute names from {@link #toString()}'s format,
     * returning message parameters as a comma-delimited list for {@link SOCMessage#parseMsgStr(String)}.
     * Converts robber hex coordinate to decimal from hexadecimal format.
     * @param message Params part of a message string formatted by {@link #toString()}; not {@code null}
     * @return Message parameters without attribute names, or {@code null} if params are malformed
     * @since 2.4.50
     */
    public static String stripAttribNames(String message)
    {
        String s = SOCMessage.stripAttribNames(message);
        if (s == null)
            return null;
        int i = s.lastIndexOf(SOCMessage.sep2_char);

        StringBuilder ret = new StringBuilder();
        ret.append(s.substring(0, i + 1));
        String robberHex = s.substring(i + 1);
        ret.append(Integer.parseInt(robberHex, 16));

        return ret.toString();
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCMoveRobber:game=" + getGame() + "|playerNumber=" + p1 + "|coord="
            + ((p2 >= 0)
              ? Integer.toHexString(p2)
              : ("-" + Integer.toHexString(- p2)));
    }
}
