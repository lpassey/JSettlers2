/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2010,2013,2016-2017,2020 Jeremy D Monin <jeremy@nand.net>
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Date;


/** A TCP client's connection at a server.
 *  Reads from the net, writes atomically to the net and
 *  holds the connection data
 *<P>
 * This class has a run method, but you must start the thread yourself.
 * Constructors will not create or start a thread.
 *<P>
 * As used within JSettlers, the structure of this class has much in common
 * with {@link MemConnection}, as they both subclass {@link Connection}.
 * If you add something to one class you should probably add it to the other,
 * or to the superclass instead.
 *<P>
 * Refactored in v2.0.0 to extend {@link Connection} instead of Thread.
 *<P>
 * Before JSettlers v2.0.00, this class was called {@code Connection}.
 *
 * @author <A HREF="http://www.nada.kth.se/~cristi">Cristian Bogdan</A>
 */
@SuppressWarnings("serial")
public final class NetConnection
    extends Connection implements Runnable, Serializable, Cloneable
{
    protected final static int TIMEOUT_VALUE = 3600000; // approx. 1 hour

    private final Socket socket;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    /** Hostname of the remote end of the connection, for {@link #getHost()} */
    private String hst;

    private boolean connected = false;

    /**
     * @see #disconnectSoft()
     * @since 1.0.3
     */
    private boolean inputConnected = false;

    /** initialize the connection data */
    public NetConnection( Socket so )
    {
        hst = so.getInetAddress().getHostName();
        socket = so;
    }

    /**
     * @return Hostname of the remote end of the connection
     */
    @Override
    public String getHost()
    {
        return hst;
    }

    /** Set up the connection to read from the network; called only by the server.
     * If successful, also sets connectTime to now.
     * Before calling {@code connect()}, be sure to make a new {@link Thread}{@code (this)} and {@code start()} it
     * for the inbound reading thread.
     *<P>
     * Connection must be unnamed (<tt>{@link #getData()} == null</tt>) at this point.
     *
     * @return true if thread start was successful, false if an error occurred.
     */
    @Override
    public boolean connect( boolean debugTraffic )
    {
        if (getData() != null)
        {
            D.ebugPrintlnINFO("conn.connect() requires null getData()");
            return false;
        }

        try
        {
            this.debugTraffic = debugTraffic;
            socket.setSoTimeout( TIMEOUT_VALUE );
            in = new DataInputStream( socket.getInputStream() );
            out = new DataOutputStream( socket.getOutputStream() );
            connected = true;
            inputConnected = true;
            connectTime = new Date();
        }
        catch (Exception e)
        {
            D.ebugPrintlnINFO("IOException in Connection.connect (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;
            disconnect();

            return false;
        }

        return true;
    }

    @Override
    public void setAccepted() throws IllegalStateException
    {
        accepted.set( true );
    }

    /**
     * Is input available now, without blocking?
     * Same idea as {@link DataInputStream#available()}.
     * @since 1.0.5
     */
    @Override
    public boolean isInputAvailable()
    {
        try
        {
            return inputConnected && (0 < in.available());
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Inbound reading thread: continuously read from the net.
     *<P>
     * When starting the thread, {@link #getData()} must be null;
     * {@link #connect(boolean)} mentions and checks that, but {@code connect()} is a different thread.
     */
    @Override
    public void run()
    {
        Thread.currentThread().setName(getName());  // connection-remotehostname-portnumber
        // won't throw IllegalArgumentException, because conn is unnamed at this point; getData() is null

        try
        {
            if (inputConnected)
            {
                final String msgStr = in.readUTF();  // blocks until next message is available
                messageDispatcher.dispatchFirst( SOCMessage.toMsg(msgStr), this );
            }
            while (inputConnected)
            {
                // readUTF max message size is 65535 chars, modified utf-8 format
                final String msgStr = in.readUTF();  // blocks until next message is available
                SOCMessage socMessage = SOCMessage.toMsg(msgStr);
                if (debugTraffic || D.ebugIsEnabled() || socMessage.debugTraffic())
                {
                    soc.debug.D.ebugPrintlnINFO( String.format( " IN - [%s] %s - %s",
                        Thread.currentThread().getName(), data, socMessage.toString() ));
                }
                if (socMessage instanceof SOCDisconnect)
                {
                   disconnect();    // sets inputConnected = false, so this thread will now terminate
                }
                else
                {
                    messageDispatcher.dispatch( socMessage, this );
                }
            }
        }
        catch (Exception e)
        {
            inputConnected = false;
            connected = false;
            D.ebugPrintlnINFO("Exception in NetConnection.run (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            if (! connected)
            {
                return;  // Don't set error twice
            }

            error = e;
        }
    }

    /**
     * Send this message over a network socket connection.
     *<P>
     * Because the connection protocol uses {@link DataOutputStream#writeUTF(String)},
     * {@code str} must be no longer than 65535 bytes when encoded into {@code UTF-8}
     * (which is not Java's internal string encoding): See {@link Connection#MAX_MESSAGE_SIZE_UTF8}.
     *<P>
     * <B>Threads:</B> Safe to call from any thread.
     *
     * @param socMessage Data to send
     */
    @Override
    public final void send( SOCMessage socMessage )
    {
        if (debugTraffic || D.ebugIsEnabled() || socMessage.debugTraffic())
            soc.debug.D.ebugPrintlnINFO(String.format( "OUT [%s] - %s - %s",
                Thread.currentThread().getName(), data, socMessage.toString() ));
        putForReal( socMessage.toCmd() );
    }

    /**
     * writes a string as UTF8 to a network socket
     *
     * @param str Data to send
     *
     * @return True if sent, false if error
     *         (and sets {@link #error})
     */
    private boolean putForReal(final String str)
    {
        boolean rv = putAux(str);

        if (! rv)
        {
             return false;
        }
        else
        {
            return true;
        }
    }

    /** put a message on the net
     * @return true for success, false and disconnects on failure
     *         (and sets {@link #error})
     */
    private boolean putAux(final String str)
    {
        if ((error != null) || ! connected)
        {
            return false;
        }

        try
        {
            out.writeUTF(str);
                // throws UTFDataFormatException (an IOException) if string length > 65535 in UTF-8
        }
        catch (IOException e)
        {
            D.ebugPrintlnINFO("IOException in NetConnection.putAux (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;

            return false;
        }
        catch (Exception ex)
        {
            D.ebugPrintlnINFO("generic exception in NetConnection.putAux");

            if (D.ebugOn)
            {
                ex.printStackTrace(System.out);
            }

            return false;
        }

        return true;
    }

    /** close the socket, stop the reader; called after conn is removed from server structures */
    @Override
    public void disconnect()
    {
        inputConnected = false;
        if (! connected)
            return;  // <--- Early return: Already disconnected ---

        D.ebugPrintlnINFO("DISCONNECTING " + data);
        connected = false;
        send( new SOCDisconnect());
        try
        {
            if (out != null)
            {
                try
                {
                    out.flush();
                }
                catch( IOException ignored ) {}
            }
            myRunner.interrupt();   // shut down the dispatcher thread, if it is still running.
            if (socket != null)
                socket.close();
        }
        catch (IOException e)
        {
            D.ebugPrintlnINFO("IOException in Connection.disconnect (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;
        }

//        socket = null;
        in = null;
        out = null;
    }

    /**
     * Accept no further input, allow output to drain, don't immediately close the socket.
     * Once called, {@link #isConnected()} will return false, even if output is still being
     * sent to the other side.
     * @since 1.0.3
     */
    @Override
    public void disconnectSoft()
    {
        if (! inputConnected)
            return;

        D.ebugPrintlnINFO("DISCONNECTING(SOFT) " + data);
        inputConnected = false;

        if (out != null)
            try
            {
                out.flush();
            }
            catch (IOException e) {}
    }

    /**
     * Are we currently connected and active?
     */
    @Override
    public boolean isConnected()
    {
        return connected && inputConnected;
    }

    /**
     * Get our connection thread name for debugging.  Also used by {@link #toString()}.
     * @return "connection-" + <em>remotehostname-portnumber</em>, or "connection-(null)-" + {@link #hashCode()}
     * @since 2.0.0
     */
    private String getName()
    {
        if ((hst != null) && (socket != null))
            return "connection-" + hst + "-" + socket.getPort();
        else
            return "connection-(null)-" + hashCode();
    }

    /**
     * For debugging, toString includes data.toString and {@link #getName()}.
     * @since 1.0.5.2
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder("Connection[");
        if (data != null)
            sb.append(data);
        else
            sb.append(super.hashCode());
        sb.append('-');
        sb.append(getName());  // connection-hostname-portnumber
        sb.append(']');
        return sb.toString();
    }
}
