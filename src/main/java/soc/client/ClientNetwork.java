/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2019-2020 Jeremy D Monin <jeremy@nand.net>
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
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.client;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Locale;

import soc.baseclient.SOCDisplaylessPlayerClient;
import soc.baseclient.ServerConnectInfo;
import soc.communication.Connection;
import soc.communication.NetConnection;
// import soc.disableDebug.D;
import soc.game.SOCGame;
import soc.game.SOCGameOptionSet;
import soc.message.SOCJoinGame;
import soc.message.SOCMessage;
import soc.message.SOCNewGameWithOptionsRequest;
import soc.message.SOCRejectConnection;
import soc.message.SOCVersion;

// TODO: We need to break this dependency!
import soc.server.SOCServer;
import soc.server.genericServer.MemServerSocket;


import soc.util.SOCFeatureSet;
import soc.util.Version;

/**
 * Helper object to encapsulate and deal with {@link SOCPlayerClient}'s network connectivity.
 *<P>
 * Local practice server (if any) is started in {@link #startPracticeGame(String, SOCGameOptionSet)}.
 * Local tcp server (if any) is started in {@link #initLocalServer(int)}.
 *<br>
 * Messages from server to client are received in either {@link NetReadTask} or {@link LocalStringReaderTask},
 * which call the client's {@link MessageHandler#handle(SOCMessage, boolean)}.
 *<br>
 * Messages from client to server are formed in {@link GameMessageSender} or other classes,
 * which call back here to send to the server via {@link #putNet(String)} or {@link #putPractice(String)}.
 *<br>
 * Network shutdown is {@link #disconnect()} or {@link #dispose()}.
 *<P>
 * Before v2.0.00, most of these fields and methods were part of the main {@link SOCPlayerClient} class.
 *
 * @author Paul Bilnoski &lt;paul@bilnoski.net&gt;
 * @since 2.0.00
 * @see SOCPlayerClient#getNet()
 */
/*package*/ class ClientNetwork
{
    /**
     * Default tcp port number 8880 to listen, and to connect to remote server.
     * Should match SOCServer.SOC_PORT_DEFAULT.
     *<P>
     * 8880 is the default SOCPlayerClient port since jsettlers 1.0.4, per cvs history.
     * @since 1.1.00
     */
    public static final int SOC_PORT_DEFAULT = 8880;

    /**
     * Timeout for initial connection to server; default is 6000 milliseconds.
     */
    public static int CONNECT_TIMEOUT_MS = 6000;

    /**
     * The client we're communicating for.
     * @see #mainDisplay
     */
    private final SOCPlayerClient client;

    /**
     * MainDisplay for our {@link #client}, to display information and perform callbacks when needed.
     * Set after construction by calling {@link #setMainDisplay(MainDisplay)}.
     */
    private MainDisplay mainDisplay;

    /**
     * Server connection info. {@code null} until {@link #connect(String, int)} is called.
     * Unlike {@code connect} params, localhost is not represented here as {@code null}:
     * see {@link ServerConnectInfo#hostname} javadoc.
     *<P>
     * Versions before 2.2.00 instead had {@code host} and {@code port} fields.
     * @since 2.2.00
     */
    protected ServerConnectInfo serverConnectInfo;

    /**
     * Client-hosted TCP server. If client is running this server, it's also connected
     * as a client, instead of being client of a remote server.
     * Started via {@link SwingMainDisplay#startLocalTCPServer(int)}.
     * {@link #practiceServer} may still be activated at the user's request.
     * Note that {@link SOCGame#isPractice} is false for localTCPServer's games.
     * @since 1.1.00
     */
    SOCServer localTCPServer = null;

    /**
     * Features supported by this built-in JSettlers client.
     * @since 2.0.00
     */
    private final SOCFeatureSet cliFeats;
    {
        cliFeats = new SOCFeatureSet(false, false);
        cliFeats.add(SOCFeatureSet.CLIENT_6_PLAYERS);
        cliFeats.add(SOCFeatureSet.CLIENT_SEA_BOARD);
        cliFeats.add(SOCFeatureSet.CLIENT_SCENARIO_VERSION, Version.versionNumber());
    }

    /**
     * Any network error (TCP communication) received while connecting
     * or sending messages in {@link #putNet(String)}, or null.
     * If {@code ex != null}, putNet will refuse to send.
     *<P>
     * The exception's {@link Throwable#toString() toString()} including its
     * {@link Throwable#getMessage() getMessage()} may be displayed to the user
     * by {@link SOCPlayerClient#shutdownFromNetwork()}; if throwing an error that the user
     * should see, be sure to set the detail message.
     * @see #ex_P
     */
    Exception ex = null;

    /**
     * Practice-server error (stringport pipes), or null.
     *<P>
     * Before v2.0.00 this field was {@code ex_L} (Local server).
     * @see #ex
     * @since 1.1.00
     */
    Exception ex_P = null;

    /**
     * For debug, our last message sent over the net.
     *<P>
     * Before v1.1.00 this field was {@code lastMessage}.
     * @see #lastMessage_P
     * /
     protected String lastMessage_N;
    */

    /**
     * For debug, our last message sent to practice server (stringport pipes).
     *<P>
     * Before v2.0.00 this field was {@code lastMessage_L} (Local server).
     * @see #lastMessage_N
     * @since 1.1.00
     */
//    protected SOCMessage lastMessage_P;

    /**
     * Server for practice games via {@link #prCli}; not connected to the network,
     * not suited for hosting multi-player games. Use {@link #localTCPServer}
     * for those.
     * SOCMessages of games where {@link SOCGame#isPractice} is true are sent
     * to practiceServer.
     *<P>
     * Null before it's started in {@link SOCPlayerClient#startPracticeGame()}.
     * @since 1.1.00
     */
    protected SOCServer practiceServer = null;

    /**
     * Create our client's ClientNetwork.
     * Before using the ClientNetwork, caller client must construct their GUI
     * and call {@link #setMainDisplay(MainDisplay)}.
     * Then, call {@link #connect(String, int)}.
     */
    public ClientNetwork( SOCPlayerClient playerClient )
    {
        if (playerClient == null)
            throw new IllegalArgumentException("client is null");
        client = playerClient;
    }

    /**
     * Set our MainDisplay; must be done after construction.
     * @param md  MainDisplay to use
     * @throws IllegalArgumentException if {@code md} is {@code null}
     */
    public void setMainDisplay(final MainDisplay md)
        throws IllegalArgumentException
    {
        if (md == null)
            throw new IllegalArgumentException("null");

        mainDisplay = md;
    }

    /** Shut down the local TCP server (if any) and disconnect from the network. */
    public void dispose()
    {
        shutdownLocalServer();
        disconnect();
    }

    /**
     * Start a practice game.  If needed, create and start {@link #practiceServer}.
     * @param practiceGameName  Game name
     * @param gameOpts  Game options, or {@code null}
     * @return True if the practice game request was sent, false if there was a problem
     *         starting the practice server or client
     * @since 1.1.00
     */
    public boolean startPracticeGame( final String practiceGameName, final SOCGameOptionSet gameOpts )
    {
        if (practiceServer == null)
        {
            try
            {
                if (Version.versionNumber() == 0)
                {
                    throw new IllegalStateException("Packaging error: Cannot determine JSettlers version");
                }

                practiceServer = new SOCServer(SOCServer.PRACTICE_STRINGPORT, SOCServer.SOC_MAXCONN_DEFAULT, null, null);
                practiceServer.setPriority(5);  // same as in SOCServer.main
                practiceServer.start();
            }
            catch (Throwable th)
            {
                mainDisplay.showErrorDialog
                    (client.strings.get("pcli.error.startingpractice") + "\n" + th,  // "Problem starting practice server:"
                     client.strings.get("base.cancel"));

                return false;
            }
        }

        Connection prCli = client.getConnection();

        if (prCli == null)
        {
            try
            {
                prCli = MemServerSocket.connectTo(SOCServer.PRACTICE_STRINGPORT);
                client.setConnection( prCli );
                prCli.startMessageProcessing( client.getMessageHandler() );  // Reader will start its own thread

                // Send VERSION right away
                sendVersion();

                // Practice server supports per-game options
                mainDisplay.enableOptions();
            }
            catch (ConnectException e)
            {
                ex_P = e;

                return false;
            }
        }

        // Ask internal practice server to create the game
        if (gameOpts == null)
            prCli.send(new SOCJoinGame(client.practiceNickname, "", SOCMessage.EMPTYSTR, practiceGameName));
        else
            prCli.send(new SOCNewGameWithOptionsRequest( client.practiceNickname, "", SOCMessage.EMPTYSTR,
                practiceGameName, gameOpts.getAll()));

        return true;
    }

    /** Shut down the local TCP server. */
    public void shutdownLocalServer()
    {
        if ((localTCPServer != null) && (localTCPServer.isUp()))
        {
            localTCPServer.stopServer();
            localTCPServer = null;
        }
    }

    /**
     * Create and start the local TCP server on a given port.
     * If startup fails, show a {@link NotifyDialog} with the error message.
     * @return True if started, false if not
     */
    public boolean initLocalServer(int tport)
    {
        try
        {
            localTCPServer = new SOCServer(tport, SOCServer.SOC_MAXCONN_DEFAULT, null, null);
            localTCPServer.setPriority(5);  // same as in SOCServer.main
            localTCPServer.start();
        }
        catch (Throwable th)
        {
            mainDisplay.showErrorDialog
                (client.strings.get("pcli.error.startingserv") + "\n" + th,  // "Problem starting server:"
                 client.strings.get("base.cancel"));

            return false;
        }

        return true;
    }

    /**
     * Port number of the tcp server we're a client of,
     * or default {@link #SOC_PORT_DEFAULT} if not {@link #isConnected()}.
     * @see #getHost()
     */
    public int getPort()
    {
        return ( client.isConnected()) ? serverConnectInfo.port : SOC_PORT_DEFAULT;
    }

    /**
     * Hostname of the tcp server we're a client of,
     * from {@link ServerConnectInfo#hostname},
     * or {@code null} if not {@link #isConnected()}.
     * @see #getPort()
     */
    public String getHost()
    {
        return (client.isConnected()) ? serverConnectInfo.hostname : null;
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
     *
     * @param host  Server host to connect to, or {@code null} for localhost
     * @param port  Server TCP port to connect to; the default server port is {@link ClientNetwork#SOC_PORT_DEFAULT}.
     * @throws IllegalStateException if already connected
     *     or if {@link Version#versionNumber()} returns 0 (packaging error)
     * @see soc.server.SOCServer#newConnection1(Connection)
     */
    public synchronized void connect(final String host, final int port)
        throws IllegalStateException
    {
        if (client.isConnected())
        {
            throw new IllegalStateException
                ("Already connected to " + (serverConnectInfo.hostname != null ? serverConnectInfo.hostname : "localhost")
                 + ":" + serverConnectInfo.port);
        }

        if (Version.versionNumber() == 0)
        {
            throw new IllegalStateException("Packaging error: Cannot determine JSettlers version");
        }

        ex = null;
        final String hostString = (host != null) ? host : "localhost";
        serverConnectInfo = new ServerConnectInfo(hostString, port, null);

        System.out.println(/*I*/"Connecting to " + hostString + ":" + port/*18N*/);  // I18N: Not localizing console output yet
        mainDisplay.setMessage
            (client.strings.get("pcli.message.connecting.serv"));  // "Connecting to server..."

        try
        {
            if (client.gotPassword)
            {
                mainDisplay.setPassword(client.password);
                    // when ! gotPassword, SwingMainDisplay.getPassword() will read pw from there
                client.gotPassword = false;
            }

            final SocketAddress srvAddr = (host != null)
                ? new InetSocketAddress(host, port)
                : new InetSocketAddress(InetAddress.getByName(null), port);  // loopback
            Socket sock = new Socket();
            sock.connect( srvAddr, CONNECT_TIMEOUT_MS );
            NetConnection connection = new NetConnection( sock );
            connection.connect(); // wires up data streams to already connected socket.
            client.setConnection( connection );
            connection.startMessageProcessing( client.getMessageHandler() );
            // send VERSION right away (1.1.06 and later)
            sendVersion();
        }
        catch (Exception e)
        {
            ex = e;
            String msg = client.strings.get("pcli.error.couldnotconnect", ex);  // "Could not connect to the server: " + ex
            System.err.println(msg);
            mainDisplay.showErrorPanel(msg, (ex_P == null));

            if (client.isConnected())
            {
                client.disconnect();
            }
            serverConnectInfo = null;
        }
    }

    /**
     * Disconnect from the net (client of remote server).
     * If a problem occurs, sets {@link #ex}.
     * @see #dispose()
     */
    protected synchronized void disconnect()
    {
        if (client.isConnected())
            // reader will die once 'connected' is false, and socket is closed
            client.disconnect();
    }

    /**
     * Construct and send a {@link SOCVersion} message during initial connection to a server.
     * Version message includes features and locale in 2.0.00 and later clients; v1.x.xx servers will ignore them.
     *<P>
     * If debug property {@link SOCPlayerClient#PROP_JSETTLERS_DEBUG_CLIENT_FEATURES PROP_JSETTLERS_DEBUG_CLIENT_FEATURES}
     * is set, its value is sent instead of {@link #cliFeats}.{@link SOCFeatureSet#getEncodedList() getEncodedList()}.
     * Then if debug property
     * {@link SOCDisplaylessPlayerClient#PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P}
     * is set, its value is appended to client features as {@code "com.example.js."} + gameopt3p.
     *
     * @param isPractice  True if sending to client's practice server with {@link #putPractice(String)},
     *     false if to a TCP server with {@link #putNet(String)}.
     * @since 2.0.00
     */
    private void sendVersion()
    {
        String feats = System.getProperty(SOCPlayerClient.PROP_JSETTLERS_DEBUG_CLIENT_FEATURES);
        if (feats == null)
            feats = cliFeats.getEncodedList();
        else if (feats.length() == 0)
            feats = null;

        String gameopt3p = System.getProperty(SOCDisplaylessPlayerClient.PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P);
        if (gameopt3p != null)
        {
            gameopt3p = "com.example.js." + gameopt3p.toUpperCase(Locale.US) + ';';
            if (feats != null)
                feats = feats + gameopt3p;
            else
                feats = ';' + gameopt3p;
        }

        final SOCMessage msg = new SOCVersion( Version.versionNumber(), Version.version(),
            Version.buildnum(), feats, client.cliLocale.toString());

        client.getConnection().send(msg);
    }

    /**
     * Are we running a local tcp server?
     * @see #getLocalServerPort()
     * @see #anyHostedActiveGames()
     * @since 1.1.00
     */
    public boolean isRunningLocalServer()
    {
        return localTCPServer != null;
    }

    /**
     * Look for active games that we're hosting (state >= START1A, not yet OVER).
     *
     * @return If any hosted games of ours are active
     * @see MainDisplay#hasAnyActiveGame(boolean)
     * @see SwingMainDisplay#findAnyActiveGame(boolean)
     * @since 1.1.00
     */
    public boolean anyHostedActiveGames()
    {
        if (localTCPServer == null)
            return false;

        Collection<String> gameNames = localTCPServer.getGameNames();

        for (String tryGm : gameNames)
        {
            int gs = localTCPServer.getGameState(tryGm);
            if ((gs < SOCGame.OVER) && (gs >= SOCGame.START1A))
            {
                return true;  // Active
            }
        }

        return false;  // No active games found
    }
}

