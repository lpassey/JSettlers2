package soc.baseclient;

import soc.client.*;
import soc.debug.D;
import soc.game.SOCGame;
import soc.game.SOCGameOptionSet;
import soc.message.SOCJoinGame;
import soc.message.SOCMessage;
import soc.message.SOCNewGameWithOptionsRequest;
import soc.server.SOCServer;
import soc.server.genericServer.StringConnection;
import soc.server.genericServer.StringServerSocket;
import soc.util.SOCGameList;

import java.io.IOException;
import java.net.ConnectException;

public class InProcessConnection extends ServerConnection
{
    SOCServer inProcServer;

    /**
     * Create our client's connection to an in-process game server
     * Before using this connection, caller client must construct their GUI
     * and call {@link SOCFullClient#setMainDisplay(MainDisplay)}.
     * Then, call {@link SOCFullClient#connect(String, int)}.
     *
     * @param client The SOCFullClient that owns this connection. Should
     *               only be one.
     */
    public InProcessConnection( SOCFullClient client )
    {
        super( client );
    }

    /**
     * Are we connected to an in-process server?
     */
    public synchronized boolean isConnected()
    {
        return null != inProcServer;
    }


    /**
     * Client in process connection to {@link #inProcServer the in-process server}.
     * Null before it's started in {@link SOCFullClient#startPracticeGame()}.
     *<P>
     * Last message is in {@link #lastMessage}; any error is in {@link #ex}.
     * @since 1.1.00
     */
    protected StringConnection strConn = null;

    /**
     * write a message to the practice server. {@link SOCFullClient#localTCPServer}
     * is not the same as the practice server; use {@link SocketConnection#send(String)}
     * to send a message to the local TCP server.
     *<P>
     * Before version 1.1.14, this was <tt>putLocal</tt>.
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     * @see GameMessageSender#put(String, boolean)
     * @throws IllegalArgumentException if {@code s} is {@code null}
     * @see SocketConnection#send(String)
     * @since 1.1.00
     */
    @Override
    public synchronized boolean send(String s)
            throws IllegalArgumentException
    {
        if (s == null)
            throw new IllegalArgumentException("null");

        lastMessage = s;

        if ((lastException != null) || ! strConn.isConnected())
        {
            return false;
        }

        if (client.debugTraffic || D.ebugIsEnabled())
            soc.debug.D.ebugPrintlnINFO("OUT L- " + SOCMessage.toMsg(s));

        strConn.put(s);

        return true;
    }

    /*
        get a game directly from the local server's game list.
     */
    public SOCGame getGame( String gameName ) 
    {
        if (null != inProcServer)
            return inProcServer.getGame( gameName );
        return null;
    }

    public SOCGameList getGames()
    {
        if (null != inProcServer)
            return inProcServer.getGameList();
        return null;
    }

    /**
     * Start a practice game.  If needed, create and start {@link #inProcServer}.
     * @param practiceGameName  Game name
     * @param gameOpts  Game options, or {@code null}
     * @return True if the practice game request was sent, false if there was a problem
     *         starting the practice server or client
     * @since 1.1.00
     */
    public boolean startPracticeGame( final String practiceGameName, final SOCGameOptionSet gameOpts)
    {
        if (inProcServer == null)
        {
            try
            {
                inProcServer = new SOCServer(SOCServer.PRACTICE_STRINGPORT, SOCServer.SOC_MAXCONN_DEFAULT, null, null);
                inProcServer.setPriority(5);  // same as in SOCServer.main
                inProcServer.start();
            }
            catch (Throwable th)
            {
                getClient().showErrorDialog(getClient().getString("pcli.error.startingpractice") + "\n" + th, false );
                return false;
            }
        }

        if (strConn == null)
        {
            try
            {
                strConn = StringServerSocket.connectTo(SOCServer.PRACTICE_STRINGPORT);
                new LocalStringReaderTask( strConn );  // Reader will start its own thread

                // Send VERSION right away
                sendVersion();

                // Practice server supports per-game options
                getClient().enableOptions();
            }
            catch (ConnectException e)
            {
                lastException = e;

                return false;
            }
        }

        // Ask internal practice server to create the game
        if (gameOpts == null)
            send( SOCJoinGame.toCmd(client.getNickname(), "", SOCMessage.EMPTYSTR, practiceGameName));
        else
            send( SOCNewGameWithOptionsRequest.toCmd(
                    client.getNickname(), "", SOCMessage.EMPTYSTR, practiceGameName, gameOpts.getAll()));

        return true;
    }

    /**
     * casts the super class "client" variable to a SOCFullClient instance.
     * @return the client instance
     */
    private SOCFullClient getClient()
    {
        return (SOCFullClient) client;
    }

    public SOCGameOptionSet getGameOptions( String gameName )
    {
        if (null != inProcServer)
            return inProcServer.getGameOptions( gameName );
        return null;
    }

    /**
     * For practice games, reader thread to get messages from the
     * practice server to be treated and reacted to.
     *<P>
     * Before v2.0.00 this class was {@code SOCPlayerClient.SOCPlayerLocalStringReader}.
     *
     * @author jdmonin
     * @since 1.1.00
     */
    class LocalStringReaderTask implements Runnable
    {
        StringConnection locl;

        /**
         * Start a new thread and listen to practice server.
         *
         * @param prConn Active connection to practice server
         */
        protected LocalStringReaderTask(StringConnection prConn)
        {
            locl = prConn;

            Thread thr = new Thread(this);
            thr.setDaemon(true);
            thr.start();
        }

        /**
         * Continuously read from the practice string server in a separate thread.
         * If disconnected or an {@link IOException} occurs, calls
         * {@link SOCPlayerClient#shutdownFromNetwork()}.
         */
        public void run()
        {
            Thread.currentThread().setName("cli-stringread");  // Thread name for debug
            try
            {
                final InProcMessageHandler handler = new InProcMessageHandler( (SOCFullClient) client );

                while (locl.isConnected())
                {
                    String s = locl.readNext();
                    SOCMessage msg = SOCMessage.toMsg(s);

                    if (msg != null)
                        handler.handle( msg , false );
                    else if (client.debugTraffic)
                        soc.debug.D.ebugERROR("Could not parse practice server message: " + s);
                }
            }
            catch (IOException e)
            {
                // purposefully closing the socket brings us here too
                if (locl.isConnected())
                {
                    lastException = e;
                    System.out.println("could not read from practice server: " + e);  // I18N: Not localizing console output yet
                    client.shutdownFromNetwork();
                }
            }
        }
    }  // nested class LocalStringReaderTask
}
