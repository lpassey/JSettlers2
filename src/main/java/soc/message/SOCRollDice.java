/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2013,2017,2020 Jeremy D Monin <jeremy@nand.net>
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

import soc.game.SOCGame;  // for javadocs only


/**
 * This message is a request from client to server; client player wants to roll the dice.
 *<P>
 * If the player can roll now, the server will respond with {@link SOCDiceResult}, {@link SOCGameState},
 * and possibly other messages, depending on the roll results and scenario/rules in effect.
 *
 * @see SOCGame#clientRequestsDiceResultsFullySent
 * @author Robert S Thomas
 */
public class SOCRollDice extends SOCMessageForGame
{
    /** Class marked for v1.1.11 with SOCMessageForGame.
     *  Over the network, fields are unchanged since v1.0.0 or earlier, per git and old cvs history. -JM
     */
    private static final long serialVersionUID = 1111L;


    /**
     * Create a RollDice message.
     *
     * @param gameName  name of game
     */
    public SOCRollDice(String gameName)
    {
        super( ROLLDICE, gameName );
    }

    /**
     * Parse the command String into a RollDice message
     *
     * @param s   the String to parse
     * @return    a RollDice message, or null if the data is garbled
     */
    public static SOCRollDice parseDataStr(String s)
    {
        return new SOCRollDice(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCRollDice:game=" + getGameName();
    }
}
