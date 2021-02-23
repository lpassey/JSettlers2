/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2019-2021 Jeremy D Monin <jeremy@nand.net>
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import soc.baseclient.SOCDisplaylessPlayerClient;
import soc.baseclient.ServerConnectInfo;
import soc.communication.Connection;
import soc.communication.MemConnection;
import soc.communication.NetConnection;
import soc.game.GameState;
import soc.game.SOCGame;
import soc.game.SOCGameOptionSet;
import soc.game.SOCScenario;
import soc.message.SOCJoinGame;
import soc.message.SOCMessage;
import soc.message.SOCNewGameWithOptionsRequest;
import soc.message.SOCRejectConnection;
import soc.message.SOCVersion;

// TODO: We need to break this dependency!
import soc.server.SOCServer;
import soc.communication.MemServerSocket;

import soc.util.SOCFeatureSet;
import soc.util.Version;

/**
 * Helper object to encapsulate and deal with {@link SOCPlayerClient}'s network connectivity.
 *<P>
 * Local practice server (if any) is started in {@link #startPracticeGame(String, SOCGameOptionSet)}.
 * Local tcp server (if any) is started in {@link #initLocalServer(int)}.
 *<br>
 * Messages from server to client are received in either {@link MemConnection#run()} or {@link NetConnection#run()},
 * which call the client's {@link MessageHandler#handle(SOCMessage, Connection)}.
 *<br>
 * Messages from client to server are formed in {@link GameMessageSender} or other classes,
 * which call back here to send to the server via {@link Connection#send(SOCMessage)}.
 *<br>
 * Network shutdown is {@link #disconnect()} or {@link #dispose()}.
 *<P>
 * Before v2.0.00, most of these fields and methods were part of the main {@link SOCPlayerClient} class.
 *
 * @author Paul Bilnoski &lt;paul@bilnoski.net&gt;
 * @since 3.0.00
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
     * Server connection info. {@code null} until {@link Connection#connect( boolean )} is called.
     * Unlike {@code connect} params, localhost is not represented here as {@code null}:
     * see {@link ServerConnectInfo#hostname} javadoc.
     *<P>
     * Versions before 2.2.00 instead had {@code host} and {@code port} fields.
     * @since 2.2.00
     */
    protected ServerConnectInfo serverConnectInfo;

    /**
     * Client-hosted server. It may be a TCP server ({@link SOCServer#SOCServer(int,int,String,String)}
     * Started via {@link SwingMainDisplay#startLocalTCPServer(int)}, or an intra-JVM server
     * {@link SOCServer#SOCServer(String)} started via {@link #startPracticeServer()}. Clients referencing
     * field need not be aware of its underlying implementation.
     * @since 1.1.00
     */
    SOCServer localServer = null;

    /**
     * Features supported by this built-in JSettlers client.
     * @since 2.0.00
     */
    private final SOCFeatureSet cliFeats;

    boolean isPracticeGame; // was the current game launched by one of the "Practice" buttons
                            // or by joining a named game on the server.

    {
        cliFeats = new SOCFeatureSet(false, false);
        cliFeats.add(SOCFeatureSet.CLIENT_6_PLAYERS);
        cliFeats.add(SOCFeatureSet.CLIENT_SEA_BOARD);
        cliFeats.add(SOCFeatureSet.CLIENT_SCENARIO_VERSION, Version.versionNumber());
    }

    /**
     * Any network error (TCP communication) received while connecting
     * or sending messages in {@link #netConnect(String, int)}, or null.
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
     * Tracks the scenarios presented by the server.
     */
    private final Map<String, SOCScenario> validScenarios = new HashMap<>();

    /**
     * Create our client's ClientNetwork.
     * Before using the ClientNetwork, caller client must construct their GUI
     * and call {@link #setMainDisplay(MainDisplay)}.
     * Then, call {@link Connection#connect( boolean )}.
     */
    public ClientNetwork( SOCPlayerClient playerClient )
    {
        if (playerClient == null)
            throw new IllegalArgumentException("client is null");
        client = playerClient;
    }

    /**
     * Set our MainDisplay; must be done after construction.
     * @param mainDisplay  MainDisplay to use
     * @throws IllegalArgumentException if {@code md} is {@code null}
     */
    public void setMainDisplay(final MainDisplay mainDisplay)
        throws IllegalArgumentException
    {
        if (mainDisplay == null)
            throw new IllegalArgumentException("null");

        this.mainDisplay = mainDisplay;
    }

    /** Shut down the local TCP server (if any) and disconnect from the network. */
    public void dispose()
    {
        shutdownLocalServer();
        disconnect();
    }

    public Map<String, SOCScenario> getValidScenarios()
    {
        return validScenarios;
    }

    void addValidScenario( SOCScenario newScenario )
    {
        try
        {
            if (null == newScenario)
                return;
            String scenarioTitle = client.strings.get( "gamescen." + newScenario.key + ".n" );
            String scenarioDesc = client.strings.get( "gamescen." + newScenario.key + ".d" );
            newScenario.setDesc( scenarioTitle, scenarioDesc );
        }
        catch( MissingResourceException ignored ) {}
        validScenarios.put( newScenario.key, newScenario );
    }

    public boolean startPracticeServer()
    {
        if (localServer == null)
        {
            validScenarios.clear();
            try
            {
                if (Version.versionNumber() == 0)
                {
                    throw new IllegalStateException("Packaging error: Cannot determine JSettlers version");
                }

                localServer = new SOCServer( Connection.JVM_STRINGPORT );
                localServer.setPriority(5);  // same as in SOCServer.main
                localServer.start();

                // Server is started. Create a connection so we can get setup information.
                memConnect();

                // Practice server supports per-game options
                mainDisplay.enableOptions();
            }
            catch (Throwable th)
            {
                mainDisplay.showErrorDialog
                    (client.strings.get("pcli.error.startingpractice") + "\n" + th,  // "Problem starting practice server:"
                        client.strings.get("base.cancel"));
                return false;
            }
        }
        return true;
    }

    /**
     * Start a practice game.  If needed, create and start {@link #localServer}.
     * @param practiceGameName  Game name
     * @param gameOpts  Game options, or {@code null}
     * @return True if the practice game request was sent, false if there was a problem
     *         starting the practice server or client
     * @since 1.1.00
     */
    public boolean startPracticeGame( final String practiceGameName, final SOCGameOptionSet gameOpts )
    {
        if (localServer == null)
        {
            if (!startPracticeServer())
                return false;
        }

        // We only get here if the "Practice" button was pushed, so it's OK to set the practice flag here.
        isPracticeGame = true;
        Connection clientConnection = client.getConnection();

        if (clientConnection instanceof MemConnection)
        {
            ((MemConnection) clientConnection).reset();
        }
        else
        {
            clientConnection.disconnect();
            clientConnection = null;
        }

        try
        {
            // practice games always run with an in-memory connection, even if connected to a
            // local server supporting TCP connections.
            clientConnection = MemServerSocket.connectTo( Connection.JVM_STRINGPORT,
                (MemConnection) clientConnection, client.debugTraffic );
            if (client.debugTraffic)
            {
                clientConnection.setData( "client" );
            }
            client.setConnection( clientConnection );
            clientConnection.startMessageProcessing( client.getMessageHandler() );  // Reader will start its own thread

            // Send VERSION right away
            sendVersion();

            // Practice server supports per-game options
            mainDisplay.enableOptions();
        }
        catch( ConnectException | ClassCastException e )
        {
            ex_P = e;

            return false;
        }

        // Ask internal practice server to create the game
        if (gameOpts == null)
            clientConnection.send(new SOCJoinGame(client.nickname, "", SOCMessage.EMPTYSTR, practiceGameName));
        else
            clientConnection.send(new SOCNewGameWithOptionsRequest( client.nickname, "", SOCMessage.EMPTYSTR,
                practiceGameName, gameOpts.getAll()));

        return true;
    }

    /** Shut down the local TCP server. */
    public void shutdownLocalServer()
    {
        if ((localServer != null) && (localServer.isUp()))
        {
            localServer.stopServer();
            localServer = null;
        }
    }

    /**
     * Create and start the local TCP server on a given port.
     * If startup fails, show a {@link NotifyDialog} with the error message.
     * @return True if started, false if not
     */
    public boolean initLocalServer( int tport )
    {
        try
        {
            localServer = new SOCServer( tport, SOCServer.SOC_MAXCONN_DEFAULT, null, null );
            localServer.setPriority(5);  // same as in SOCServer.main
            localServer.start();
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
     * or default {@link #SOC_PORT_DEFAULT} if not {@link SOCPlayerClient#isConnected()}.
     * @see #getHost()
     */
    public int getPort()
    {
        return ( client.isConnected()) ? serverConnectInfo.port : SOC_PORT_DEFAULT;
    }

    /**
     * Hostname of the tcp server we're a client of,
     * from {@link ServerConnectInfo#hostname},
     * or {@code null} if not {@link SOCPlayerClient#isConnected()}.
     * @see #getPort()
     */
    public String getHost()
    {
        return (client.isConnected()) ? serverConnectInfo.hostname : null;
    }

    /**
     * Attempts to connect to a local server. See {@link SOCPlayerClient#isConnected()} for success or
     * failure. Once connected, starts a client {@link MessageHandler} thread.
     * The first message sent from client to server is our {@link SOCVersion}.
     * Upon connection, if server is full, it sends a {@link SOCRejectConnection} message,
     * otherwise it sends {@code SOCVersion} and current channels and games ({@link soc.message.SOCChannels},
     * {@link soc.message.SOCGames}).
     *
     * @throws IllegalStateException if already connected or otherwise unable to connect
     * @see soc.server.SOCServer#newConnection1(Connection)
     */
    public synchronized void memConnect() throws IllegalStateException
    {
        if (client.isConnected())
        {
            throw new IllegalStateException
                ("Already connected to " + (serverConnectInfo.hostname != null ? serverConnectInfo.hostname : "localhost")
                    + ":" + serverConnectInfo.port);
        }
        Connection memConnection;
        try
        {
            memConnection = MemServerSocket.connectTo( Connection.JVM_STRINGPORT, null, client.debugTraffic );
        }
        catch( ConnectException e )
        {
            throw new IllegalStateException( "Could not connect to in-memory server", e );
        }
        if (client.debugTraffic)
        {
            memConnection.setData( "client" );
        }
        serverConnectInfo = new ServerConnectInfo( Connection.JVM_STRINGPORT, null);
        client.setConnection( memConnection );
        memConnection.startMessageProcessing( client.getMessageHandler() );  // Reader will start its own thread

        // Send VERSION right away
        sendVersion();
    }

    /**
     * Attempts to connect to a TCP server. See {@link SOCPlayerClient#isConnected()} for success or
     * failure. Once connected, starts a client {@link MessageHandler} thread.
     * The first message sent from client to server is our {@link SOCVersion}.
     * Upon connection, if server is full, it sends a {@link SOCRejectConnection} message,
     * otherwise it sends {@code SOCVersion} and current channels and games
     * ({@link soc.message.SOCChannels}, {@link soc.message.SOCGames}).
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
    public synchronized void netConnect( final String host, final int port )
        throws IllegalStateException
    {
        if (client.isConnected())
        {
            client.disconnect();
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
            connection.connect( client.debugTraffic ); // wires up data streams to already connected socket.
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
     * Are we running a local server?
     * @see #anyHostedActiveGames()
     * @since 1.1.00
     */
    public boolean isRunningLocalServer()
    {
        return localServer != null;
    }

    /**
     * Look for active games that we're hosting (state >= START1A, not yet GAME_OVER).
     *
     * @return If any hosted games of ours are active
     * @see MainDisplay#hasAnyActiveGame(boolean)
     * @see SwingMainDisplay#findAnyActiveGame()
     * @since 1.1.00
     */
    public boolean anyHostedActiveGames()
    {
        if (localServer == null)
            return false;

        Collection<String> gameNames = localServer.getGameNames();

        for (String tryGm : gameNames)
        {
            GameState gs = localServer.getGameState( tryGm );
            if ((gs.lt( GameState.GAME_OVER )) && (gs.gt( GameState.READY_RESET_WAIT_ROBOT_DISMISS )))
            {
                return true;  // Active
            }
        }
        return false;  // No active games found
    }
}

