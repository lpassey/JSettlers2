/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2009-2010,2014,2018,2020 Jeremy D Monin <jeremy@nand.net>
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
 * This message to all clients means that a new game has been created.
 * If the client has requested creating this game, NEWGAME will be followed
 * by JOINGAMEAUTH.
 *<P>
 * Version 1.1.06 and later:
 * Game name may include a marker prefix if the client can't join;
 * see {@link SOCGames#MARKER_THIS_GAME_UNJOINABLE}.
 * This marker will be retained within the game name returned by
 * {@link #getGame()}.
 *<P>
 * Just like {@link SOCNewGameWithOptions}, robot clients don't need to handle
 * this message type. Bots ignore new-game announcements and are asked to
 * join specific games.
 *
 * @author Robert S Thomas
 */
public class SOCNewGame extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * Create a NewGame message.
     *
     * @param ga  the name of the game; may have
     *            the {@link SOCGames#MARKER_THIS_GAME_UNJOINABLE} prefix.
     */
    public SOCNewGame(String ga)
    {
        super( NEWGAME, ga );
    }

    /**
     * Parse the command String into a NewGame message
     *
     * @param s   the String to parse
     * @return    a NewGame message
     */
    public static SOCNewGame parseDataStr(String s)
    {
        return new SOCNewGame(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCNewGame:game=" + getGame();
    }
}
