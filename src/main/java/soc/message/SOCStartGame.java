/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2013-2014,2017,2019 Jeremy D Monin <jeremy@nand.net>
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

import soc.game.SOCGame;


/**
 * From client, this message means that a player wants to start the game;
 * from server, it means that a game has just started, leaving state {@code NEW}.
 * The server sends the game's new {@link SOCGameState} before sending {@code SOCStartGame}.
 *<P>
 * In v2.0.00 and newer, from server this message optionally includes a {@link #getGameState()} field
 * instead of a separate {@link SOCGameState} message, since the state is part of the Start Game transition.
 *<P>
 * If a client joins a game in progress, it won't be sent a {@code SOCStartGame} message,
 * only the game's current {@code SOCGameState} and other parts of the game's and
 * players' current status: See {@link SOCJoinGameAuth}.
 *
 * @author Robert S. Thomas
 */
public class SOCStartGame extends SOCMessageForGame
{
    private static final long serialVersionUID = 2000L;  // last structural change v2.0.00

    /**
     * The optional {@link SOCGame} State field, or 0.
     * See {@link #getGameState()} for details.
     * @since 2.0.00
     */
    private final int gameState;

    /**
     * Create a StartGame message.
     *
     * @param gameName  the name of the game
     * @param gameState  the new turn's optional Game State such as {@link SOCGame#ROLL_OR_CARD}, or 0.
     *     Ignored from client. Values &lt; 0 are out of range and ignored (treated as 0).
     *     Must not send {@code gs} to a client older than {@link SOCGameState#VERSION_FOR_GAME_STATE_AS_FIELD}.
     */
    public SOCStartGame( final String gameName, final int gameState )
    {
        super( STARTGAME, gameName );
        this.gameState = (gameState > 0) ? gameState : 0;
    }

    /**
     * From server, get the the new turn's optional {@link SOCGame} State.
     * Ignored if sent from client. Must not be sent by server to clients older
     * than v2.0.00 ({@link SOCGameState#VERSION_FOR_GAME_STATE_AS_FIELD}) because they
     * won't parse it out and instead will treat state as part of the game name.
     * @return Game State, such as {@link SOCGame#ROLL_OR_CARD}, or 0
     * @since 2.0.00
     */
    public int getGameState()
    {
        return gameState;
    }

    /**
     * STARTGAME sep game [sep2 gameState]
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd()+ ((gameState > 0) ? sep2 + gameState : "");
    }

    /**
     * Parse the command String into a StartGame message
     *
     * @param s   the String to parse
     * @return    a StartGame message, or null if the data is garbled
     */
    public static SOCStartGame parseDataStr(String s)
    {
        try
        {
            String ga;   // the game name
            int gs = 0;  // the game state

            StringTokenizer st = new StringTokenizer(s, sep2);

            ga = st.nextToken();
            if (st.hasMoreTokens())
                gs = Integer.parseInt(st.nextToken());

            return new SOCStartGame(ga, gs);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCStartGame:game=" + getGame() + ((gameState != 0) ? "|gameState=" + gameState : "");
    }

}
