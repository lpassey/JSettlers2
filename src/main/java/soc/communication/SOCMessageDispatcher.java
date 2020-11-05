/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2016-2020 Jeremy D Monin <jeremy@nand.net>
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
package soc.communication;

import soc.message.SOCMessage;

/**
 * Dispatcher for all incoming messages from clients.
 *<br />
 * This interface was created in v2.0.00 refactoring from the {@code Server.processCommand(..)} method.
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 2.0.00
 */
public interface SOCMessageDispatcher
{
    /**
     * Remove a queued incoming message from a client, and handle it.
     * Called from the message handler thread of {@link Connection}.
     *<P>
     * <em>Do not block or sleep</em> in any derived code because this is single-threaded.
     * Any slow or lengthy work for a message should be done on other threads.
     *<P>
     * {@code dispatch(..)} must catch any exception thrown by conditions or
     * bugs in server or gameName code it calls.
     *<P>
     * The first message from a peer is dispatched by
     * {@link dispatchFirst(SOCMessage, Connection)}.
     *<P>
     *<B>Security Note:</B> When there is a choice, always use local information
     * over information from the message.  For example, use the nickname from the connection to get the player
     * information rather than the player information from the message.  This makes it harder to send false
     * messages making players do things they didn't want to do.
     *
     * @param message Message from the client. Will never be {@code null}.
     * @param connection Connection (peer) sending this message. Will never be {@code null}.
     * @throws IllegalStateException if not ready to dispatch because some
     *    initialization method needs to be called first;
     */
    void dispatch( SOCMessage message, Connection connection )
        throws IllegalStateException;

    void dispatchFirst( SOCMessage message, Connection connection )
        throws IllegalStateException;
}

