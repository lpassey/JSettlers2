/**
 * Local (MemConnection) communication system.
 * This file Copyright (C) 2007-2010,2012-2013,2016-2017,2020 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012 Paul Bilnoski <paul@bilnoski.net>
 * Portions of this file Copyright (C) 2016 Alessandro D'Ottavio
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

import soc.disableDebug.D;
import soc.message.SOCDisconnect;
import soc.message.SOCMessage;
import soc.server.genericServer.Server;

import java.io.EOFException;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Symmetric buffered connection sending strings between two local peers.
 * Uses vectors and thread synchronization, no actual network traffic.
 * When using this class from the server (not client), after the constructor
 * call {@link #setServer(Server)}.
 *<P>
 * This class has a run method, but you must start the thread yourself.
 * Constructors will not create or start a thread.
 *<P>
 * As used within JSettlers, the structure of this class has much in common
 * with {@link NetConnection}, as they both subclass {@link Connection}.
 * If you add something to one class you should probably add it to the other,
 * or to the superclass instead.
 *
 *<PRE>
 *  1.0.0 - 2007-11-18 - initial release, becoming part of jsettlers v1.1.00
 *  1.0.1 - 2008-06-28 - add getConnectTime
 *  1.0.2 - 2008-07-30 - check if s already null in disconnect
 *  1.0.3 - 2008-08-08 - add disconnectSoft, getVersion, setVersion (jsettlers 1.1.00 release)
 *  1.0.4 - 2008-09-04 - add appData
 *  1.0.5 - 2009-05-31 - add isVersionKnown, setVersion(int,bool), setVersionTracking,
 *                       isInputAvailable, callback to processFirstCommand,
 *                       wantsHideTimeoutMessage, setHideTimeoutMessage;
 *                       common constructor code moved to init().
 *  1.0.5.1- 2009-10-26- javadoc warnings fixed
 *  1.0.5.2- 2010-04-05- add toString for debugging
 *  1.2.0 - 2017-06-03 - {@link #setData(String)} now takes a String, not Object.
 *  2.0.0 - 2017-11-01 - Rename StringConnection -> Connection, NetStringConnection -> NetConnection,
 *                       LocalStringConnection -> StringConnection.
 *                       Connection is now a superclass, not an interface.
 *  2.1.0 - 2020-01-09 - Only server-side changes: See {@link SOCServerSocket}
 *  2.3.0 - 2020-04-27 - no change in this file
 *  2.4.5 - 2020-07-17 - put: fix cosmetic "IllegalStateException: Not accepted by server yet" seen when
 *                       sending message during disconnect/server shutdown
 *</PRE>
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.00
 */
public class MemConnection extends Connection
{
    private LinkedBlockingQueue<SOCMessage> waitQueue = new LinkedBlockingQueue<>();

    private MemConnection ourPeer;

    /**
     * Create a new, unused MemConnection.
     *<P>
     * This class has a run method, but you must start the thread yourself.
     * Constructors will not create or start a thread.
     */
    public MemConnection() {}

    /**
     * Constructor for an existing peer
     *<P>
     * This class has a run method, but you must start the thread yourself.
     * Constructors will not create or start a thread.
     *
     * @param peer The MemConnection instance to be paired with this connection
     *             and with which this connection will communicate.
     *
     * @throws EOFException If peer is at EOF already
     * @throws IllegalArgumentException if peer is null, or already has a peer.
     */
    public MemConnection( MemConnection peer) throws EOFException
    {
        if (peer == null)
            throw new IllegalArgumentException("peer null");
        if (peer.ourPeer != null)
            throw new IllegalArgumentException("peer already has a peer");
        if (peer.isOutEOF() || peer.isInEOF())
            throw new EOFException("peer EOF at constructor");

        this.ourPeer = peer;
        peer.ourPeer = this;
    }

    /**
     *
     * @return
     */
    public boolean reset()
    {
        if (null == myRunner || !myRunner.isAlive())
        {
            out_setEOF.set( false );
            in_reachedEOF.set( false );
            ourPeer = null;
            return true;
        }
        return false;
    }

    /**
     * Accept a message to be "treated". In the case of in-memory connections this will place the
     * message in the inbound queue for the MessageHandler to deal with in a separate thread.
     * In the case of a network connection, this will bundle up the message and send it over the wire.
     *
     * @param receivedMessage from the connection; will never be {@code null}
     */
    public boolean receive( SOCMessage receivedMessage )
    {
        try
        {
            waitQueue.put( receivedMessage );
            return true;
        }
        catch( InterruptedException e )
        {
            return false;
        }
    }

    /**
     * Remember, the peer's in is our out, and vice versa.
     *
     * @return Returns our peer, or null if not yet connected.
     */
    public MemConnection getPeer()
    {
        return ourPeer;
    }

     /**
     * Send data over the connection.  Does not block.
     * Ignored if setEOF() has been called.
     *<P>
     * <B>Threads:</B> Safe to call from any thread; synchronizes on internal {@code out} queue.
     *
     * @param dat Data to send
     *
     * @throws IllegalArgumentException if {@code dat} is {@code null}
     * @throws IllegalStateException if not yet accepted by server
     */
    @Override
    public void send( SOCMessage socMessage )
        throws IllegalArgumentException, IllegalStateException
    {
        if (socMessage == null)
            throw new IllegalArgumentException("null");

        if (! (accepted.get() || (data != null)))
        {
            // accepted is false before server accepts connection,
            // and after disconnect() (data != null at that point if auth'd)
            error = new IllegalStateException("Not accepted by server yet");
            throw (IllegalStateException) error;
        }
        if (isInEOF())
            return;
        if (debugTraffic || D.ebugIsEnabled())
            soc.debug.D.ebugPrintlnINFO("OUT - " + data + " - " + socMessage.toString());

        if (null != ourPeer)
            ourPeer.receive( socMessage );
    }


    /**
     * <p>Fetches the next message from this connection's message queue and dispatches it to the
     * registered message handler. The first message in the queue is dispatched separately from
     * the remaining messages for the benefit of server-side code. Client-side code typically
     * treats all messages equally.
     *</p>
     * When starting the thread, {@link #getData()} must be null.
     */
    @Override
    public void run()
    {
        Thread.currentThread().setName("connection-srv-localstring");

        try
        {
            if (! isInEOF())
            {
                final SOCMessage msgObj = waitQueue.take();  // blocks until next message is available
                messageDispatcher.dispatchFirst( msgObj, this );
            }
            while (! isInEOF())
            {
                final SOCMessage msgObj = waitQueue.take();  // blocks until next message is available
                messageDispatcher.dispatch( msgObj, this );
            }
        }
        catch (Exception e)
        {
            D.ebugPrintlnINFO("Exception in " + getClass().getSimpleName() + ".run - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }
            if (isInEOF())
            {
                return;
            }
            error = e;
        }
    }

    /**
     * Is currently accepted by a server
     *
     * @return Are we currently connected, accepted, and ready to send/receive data?
     */
    @Override
    public boolean isAccepted()
    {
        return accepted.get();
    }

    /**
     * Intended for server to call: Set our accepted flag.
     * Peer must be non-null to set accepted.
     * If our EOF is set, will not set accepted, but will not throw exception.
     * (This happens if the server socket closes while we're in its accept queue.)
     *
     * @throws IllegalStateException If we can't be, or already are, accepted
     */
    @Override
    public void setAccepted() throws IllegalStateException
    {
        if (ourPeer == null)
            throw new IllegalStateException("No peer, can't be accepted");
        if (isAccepted())
            throw new IllegalStateException("Already accepted");
        if (! (isOutEOF() || isInEOF()))
            accepted.set( true );
    }

    /**
     * Have we received an EOF marker inbound?
     */
    @Override
    public boolean isInEOF()
    {
        return in_reachedEOF.get();
    }

    /**
     * Have we closed our outbound side?
     *
     * @see #setEOF()
     */
    @Override
    public boolean isOutEOF()
    {
        return out_setEOF.get();
    }

    /**
     * Hostname of the remote side of the connection -
     * Always returns localhost; this method required for
     * the Connection interface.
     */
    @Override
    public String getHost()
    {
        if (null == data)
            return "localhost";
        return data;
    }

    /**
     * Local version; nothing special to do to start reading messages.
     * Call connect(serverSocketName) instead of this method.
     *
     * @see #connect(String)
     *
     * @return Whether we've connected and been accepted by a SOCServerSocket.
     */
    @Override
    public boolean connect()
    {
        connectTime = new Date();
        return isAccepted();
    }

    /** Are we currently connected and active? */
    @Override
    public boolean isConnected()
    {
        return isAccepted() && ! (isOutEOF() || isInEOF());
    }

    /**
     * close the connection, discard pending buffered data, set EOF.
     * Called after conn is removed from server structures.
     */
    @Override
    public void disconnect()
    {
        if (! accepted.get())
            return;  // <--- Early return: Already disconnected, or never connected ---

        D.ebugPrintlnINFO("DISCONNECTING " + data);
        // let the remote-end know we're closing
        if (null != ourPeer)
        {
            send( new SOCDisconnect());
            // It's possible that the receiving thread set ourPeer to null just like we're doing
            // here before we get a chance to do anything, so protect ourselves here.
            if (ourPeer != null)
            {
                ourPeer.ourPeer = null; // prevent infinite loops
                ourPeer.receive( new SOCDisconnect() );
                ourPeer = null;
            }
        }
        accepted.set( false );
        waitQueue.clear();

        out_setEOF.set( true );

        stopMessageProcessing();
    }

    /**
     * Accept no further input, allow output to drain, don't immediately close the socket.
     * Once called, {@link #isConnected()} will return false, even if output is still being
     * sent to the other side.
     */
    @Override
    public void disconnectSoft()
    {
        if (isInEOF())
            return;  // <--- Early return: Already stopped input and draining output ---

        // Don't check accepted; it'll be false if we're called from
        // disconnect(), and it's OK to do this part twice.

        D.ebugPrintlnINFO("DISCONNECTING(SOFT) " + data);
        in_reachedEOF.set( true );
    }

    /**
     * Is input available now, without blocking?
     * Same idea as {@link java.io.DataInputStream#available()}.
     * @since 1.0.5
     */
    @Override
    public boolean isInputAvailable()
    {
        return (! isInEOF()) && (! waitQueue.isEmpty());
    }

    /**
     * For debugging, toString includes connection name key ({@link #getData()}) if available.
     * @since 1.0.5.2
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( getClass().getSimpleName() + "[");
        if (data != null)
            sb.append(data);
        else
            sb.append(super.hashCode());
        sb.append(']');
        return sb.toString();
    }
}
