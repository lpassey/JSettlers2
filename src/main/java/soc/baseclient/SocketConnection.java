/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2019-2023 Jeremy D Monin <jeremy@nand.net>
 * Extracted in 2019 from SOCPlayerClient.java, so:
 * Portions of this file Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2012-2013 Paul Bilnoski <paul@bilnoski.net>
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
 * The maintainer of this file can be reached at github.com
 **/
package soc.baseclient;

import soc.client.*;
import soc.disableDebug.D;
import soc.message.*;
import soc.server.genericServer.Connection;
import soc.util.Version;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Helper object to encapsulate and deal with {@link SOCBaseClient}'s server connectivity.
 *<P>
 * This object deals only with connections to TCP networked servers (which may
 * be in-process, but which communicate over network sockets. A Local tcp server
 * (if any) is started in {@link SOCFullClient#initLocalServer(int)} ).
 *<br>
 * Messages from server to client are received in this object's {@link NetReadTask} object
 * which call the client's {@link PlayerMessageHandler#handle( SOCMessage )} method. The client
 * reference is set in this object's constructor.
 *<br>
 * Messages from client to server are formed in {@link GameMessageSender} or other classes,
 * which call back here to send to the server via {@link #send(String)} or {@link #putPractice(String)}.
 *<br>
 * Network shutdown is done in {@link #dispose()}, or {@link #disconnect()} for only the client part.
 *<P>
 * Before v2.0.00, most of these fields and methods were part of the main {@link SOCPlayerClient} class.
 *
 * @author Lee Passey based on code by Paul Bilnoski &lt;paul@bilnoski.net&gt;
 * @since 2.8.00
 */

public class SocketConnection extends ServerConnection
{
    /**
     * Network socket data.
     * Before v2.5.00 this field was {@code s}.
     */
    Socket sock;
    DataInputStream in;
    DataOutputStream out;
    Thread reader = null;

    /**
     * Timeout for initial network connection to server; default is 6000 milliseconds.
     */
    public static int CONNECT_TIMEOUT_MS = 6000;


    /**
     * Default tcp port number 8880 to listen, and to connect to remote server.
     * Should match SOCServer.SOC_PORT_DEFAULT.
     *<P>
     * 8880 is the default SOCPlayerClient port since jsettlers 1.0.4, per cvs history.
     * @since 1.1.00
     */
    public static final int SOC_PORT_DEFAULT = 8880;

    /**
     * When client is running a local server, interval for how often to send {@link SOCServerPing}
     * keepalive messages: 45 minutes, in milliseconds. See {@link #connect(String, int)}.
     * Should be several minutes shorter than {@link soc.server.genericServer.NetConnection#TIMEOUT_VALUE}.
     *<P>
     *     NOTE: A local server is still a network server, it's just one running in the same
     *     process space as the client.
     *<p>
     * @since 2.5.00
     */
    protected static final int PING_LOCAL_SERVER_INTERVAL_MS = 45 * 60 * 1000;

    /**
     * Are we connected to a networked server?
     * If true, {@link #serverConnectInfo} != null.
     * @see #lastException
      */
    protected boolean connected = false;

    /**
     * Are we connected to a tcp server?
     * @see #getHost()
     */
    public synchronized boolean isConnected()
    {
        return connected;
    }

    /**
     * Port number of the tcp server we're a client of,
     * or default {@link #SOC_PORT_DEFAULT} if not {@link #isConnected()}.
     * @see #getHost()
     */
    public int getPort()
    {
        return (connected) ? serverConnectInfo.port : SOC_PORT_DEFAULT;
    }

    /**
     * Hostname of the tcp server we're a client of,
     * from {@link ServerConnectInfo#hostname},
     * or {@code null} if not {@link #isConnected()}.
     * @see #getPort()
     */
    public String getHost()
    {
        return (connected) ? serverConnectInfo.hostname : null;
    }

    /**
     * Constructor
     *
     * @param client the {@link SOCBaseClient} instance associated with this connection.
     */
    public SocketConnection( SOCBaseClient client )
    {
        super( client );
    }

    /**
     * Attempts to connect to the server. See {@link #isConnected()} for success or
     * failure. Once connected, starts a {@link #reader} thread.
     * The first message sent from client to server is our {@link SOCVersion}.
     * From server to client when client connects: If server is full, it sends {@link SOCRejectConnection}.
     * Otherwise its {@code SOCVersion} and current channels and games ({@link SOCChannels}, {@link SOCGames}) are sent.
     *<P>
     * Since user login and authentication don't occur until a game or channel join is requested,
     * no username or password is needed here.
     *<P>
     * If {@code host} is {@code null}, assumes client has started a local TCP server and is now connecting to it.
     * Will start a thread here to periodically {@link SOCServerPing} that server to prevent timeouts when idle.
     *
     * @param host  Server host to connect to, or {@code null} for localhost
     * @param port  Server TCP port to connect to; the default server port is {@link #SOC_PORT_DEFAULT}.
     * @throws IllegalStateException if already connected
     *     or if {@link Version#versionNumber()} returns 0 (packaging error)
     * @see soc.server.SOCServer#newConnection1(Connection)
     */
    public synchronized void connect(final String host, final int port)
            throws IllegalStateException
    {
        if (connected)
        {
            throw new IllegalStateException(
                    "Already connected to "
                            + (serverConnectInfo.hostname != null ? serverConnectInfo.hostname : "localhost")
                            + ":" + serverConnectInfo.port);
        }

        if (Version.versionNumber() == 0)
        {
            throw new IllegalStateException("Packaging error: Cannot determine JSettlers version");
        }

        lastException = null;
        final String hostString = (host != null) ? host : "localhost";  // I18N: no need to localize this hostname
        serverConnectInfo = new ServerConnectInfo(hostString, port, null);

        try
        {
            if (client.isAuthenticated())
            {
                client.setAuthenticated( false );
            }

            final SocketAddress srvAddr = (host != null)
                    ? new InetSocketAddress(host, port)
                    : new InetSocketAddress( InetAddress.getByName(null), port);  // loopback
            sock = new Socket();
            sock.connect(srvAddr, CONNECT_TIMEOUT_MS);
            sock.setSoTimeout(0);  // ensure no read timeouts; is probably already 0 from Socket defaults
            in = new DataInputStream(sock.getInputStream());
            out = new DataOutputStream(sock.getOutputStream());
            connected = true;

            (reader = new Thread(new SocketConnection.NetReadTask((SOCPlayerClient) client, this))).start();
            // send VERSION right away (1.1.06 and later). The server will not
            // start its incoming queue until it gets this message
            sendVersion();

            if (host == null)
            {
                final Thread pinger = new Thread("cli-ping-local-srv")
                {
                    public void run()
                    {
                        final String pingCmd = new SOCServerPing(0).toCmd();

                        while(connected)
                        {
                            try
                            {
                                Thread.sleep(PING_LOCAL_SERVER_INTERVAL_MS);
                            }
                            catch (InterruptedException ignore) {}

                            send( pingCmd );
                        }
                    }
                };

                pinger.setDaemon(true);
                pinger.start();
            }
        }
        catch (Exception e)
        {
            lastException = e;

            // TODO: belongs in the calling routine.
            String msg = lastException.getLocalizedMessage();
            // client.strings.get("pcli.error.couldnotconnect", lastException);  // "Could not connect to the server: " + lastException
            System.err.println(msg);
            getClient().showErrorPanel(msg, true );

            if (connected)
            {
                disconnect();
                connected = false;
            }
            serverConnectInfo = null;

            if (in != null)
            {
                try { in.close(); } catch (Throwable th) {}
                in = null;
            }
            if (out != null)
            {
                try { out.close(); } catch (Throwable th) {}
                out = null;
            }

            sock = null;
        }
    }

    private SOCPlayerClient getClient()
    {
        return (SOCPlayerClient) client;
    }

//    public void dispose()
//    {
//        disconnect();
//    }

    /**
     * Disconnect from the net (client of remote server).
     * If a problem occurs, sets {@link #lastException}.
     * @see #dispose()
     */
    public synchronized void disconnect()
    {
        connected = false;

        // reader will die once 'connected' is false, and socket is closed

        try
        {
            sock.close();
        }
        catch (Exception e)
        {
            lastException = e;
        }
    }

    /**
     * write a message to the net
     *
     * If {@link #lastException} != null, or ! {@link #connected}, {@code putNet}
     * returns false without attempting to send the message.
     *<P>
     * This message is copied to {@link ServerConnection#lastMessage}; any error sets {@link #lastException}
     * and calls {@link SOCPlayerClient#shutdownFromNetwork()} to show the error message.
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     * @see GameMessageSender#put(String, boolean)
     */
    public synchronized boolean send(String s)
    {
        lastMessage = s;

        if ((lastException != null) || ! isConnected())
        {
            return false;
        }

        if (client.debugTraffic || D.ebugIsEnabled())
            soc.debug.D.ebugPrintlnINFO("OUT - " + SOCMessage.toMsg(s));

        try
        {
            out.writeUTF(s);
            out.flush();
        }
        catch (IOException e)
        {
            lastException = e;
            System.err.println("could not write to the net: " + lastException );  // I18N: Not localizing console output yet
            client.shutdownFromNetwork();

            return false;
        }

        return true;
    }

    /**
     * resend the last message (to the network)
     */
    public void resendNet()
    {
        if (lastMessage != null)
            send( lastMessage );
    }

    /**
     * For shutdown - Tell the server we're leaving all games.
     * If we've started a practice server, also tell that server.
     * If we've started a TCP server, tell all players on that server, and shut it down.
     *<P><em>
     * Since no other state variables are set, call this only right before
     * discarding this object or calling System.exit.
     *</em>
     * @since 1.1.00
     */
    public boolean putLeaveAll()
    {
//        final boolean canPractice = (ex_P == null);  // Can we still start a practice game?

        SOCLeaveAll leaveAllMes = new SOCLeaveAll();
        send( leaveAllMes.toCmd()) ;
//        if ((strConn != null) && ! canPractice)
//            putPractice(leaveAllMes.toCmd());

//        shutdownLocalServer();

        return true;
    }

    /**
     * A task to continuously read from the server socket.
     * Not used for talking to the practice server.
     * @see ClientNetwork.LocalStringReaderTask
     */
    static class NetReadTask implements Runnable
    {
        final SocketConnection connection;
        final SOCPlayerClient client;

        public NetReadTask(SOCPlayerClient client, SocketConnection connection)
        {
            if (null == client.messageHandler)
                throw new IllegalArgumentException( "Client message handler has not yet been initialized" );
            this.client = client;
            this.connection = connection;
        }

        /**
         * continuously read from the net in a separate thread;
         * not used for talking to the practice server.
         * If disconnected or an {@link IOException} occurs,
         * calls {@link SOCPlayerClient#shutdownFromNetwork()}.
         */
        public void run()
        {
            Thread.currentThread().setName("cli-netread");  // Thread name for debug
            try
            {
                final PlayerMessageHandler handler = (PlayerMessageHandler) client.getMessageHandler();

                // TODO: can probably be initialized in the client?
//                handler.init(client);

                while (connection.isConnected())
                {
                    String s = connection.in.readUTF();
                    SOCMessage msg = SOCMessage.toMsg(s);

                    if (msg != null)
                        handler.handle(msg, false);
                    else if (client.debugTraffic)
                        soc.debug.D.ebugERROR("Could not parse net message: " + s);
                }
            }
            catch (IOException e)
            {
                // purposefully closing the socket brings us here too
                if (connection.isConnected())
                {
                    connection.lastException = e;
                    System.out.println("could not read from the net: " + connection.lastException );  // I18N: Not localizing console output yet
                    client.shutdownFromNetwork();
                }
            }
        }
    }  // nested class NetReadTask

}
