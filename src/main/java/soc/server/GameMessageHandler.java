/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2016,2020 Jeremy D Monin <jeremy@nand.net>
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
package soc.server;

import soc.communication.Connection;

import soc.game.SOCGame;
import soc.message.SOCMessageForGame;
import soc.message.SOCMessageFromUnauthClient;

/**
 * Server interface to handle inbound game-specific messages for a type of game;
 * each game type has a {@code GameMessageHandler} and a {@link GameHandler}.
 * See {@link GameHandler} class javadoc for concepts and boundaries.
 *
 * @see GameHandler#getMessageHandler()
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 2.0.00
 */
public interface GameMessageHandler
{
    /**
     * Process one inbound command message from a client player of this game.
     *<P>
     * Some game messages (such as player sits down, or board reset voting) are handled the same for all game types.
     * These are handled at {@link SOCServer}; they should be ignored here and not appear in your switch statement.
     *<P>
     * Called from {@link soc.server.genericServer.InboundMessageQueue} message handler loop via the
     * {@link soc.communication.SOCMessageDispatcher SOCMessageDispatcher}.
     * Caller will catch any thrown Exceptions.
     *
     * @param game  Game in which client {@code c} is sending {@code msg}.
     *            Never null; from {@link SOCMessageForGame#getGameName()}.
     * @param clientMsg  Message from client {@code c}. Never null.
     * @param c    Client sending {@code msg}. Never null.
     *     {@link Connection#getData()} won't be {@code null}
     *     unless {@code mes} implements {@link SOCMessageFromUnauthClient}.
     * @return  true if processed, false if ignored or unknown message type
     */
    boolean dispatch( SOCGame game, SOCMessageForGame clientMsg, Connection c )
        throws Exception;

}
