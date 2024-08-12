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
     * Create our client's ClientNetwork.
     * Before using the ClientNetwork, caller client must construct their GUI
     * and call {@link #setMainDisplay(MainDisplay)}.
     * Then, call {@link #connect(String, int)}.
     *
     * @param client The SOCBaseClient sub class that owns this connection. Should
     *               only be one.
     */
    public InProcessConnection( SOCBaseClient client ) {
        super( client );
    }

    /**
     * Are we connected to a tcp server?
     * @see #getHost()
     */
    public synchronized boolean isConnected()
    {
        return null != inProcServer;
    }


    /**
     * Client in process connection to {@link #inProcServer the in-process server}.
     * Null before it's started in {@link #startPracticeGame()}.
     *<P>
     * Last message is in {@link #lastMessage_P}; any error is in {@link #ex_P}.
     * @since 1.1.00
     */
    protected StringConnection strConn = null;

    /**
     * write a message to the practice server. {@link #localTCPServer} is not
     * the same as the practice server; use {@link #putNet(String)} to send
     * a message to the local TCP server.
     * Use <tt>putPractice</tt> only with {@link #practiceServer}.
     *<P>
     * Before version 1.1.14, this was <tt>putLocal</tt>.
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     * @see GameMessageSender#put(String, boolean)
     * @throws IllegalArgumentException if {@code s} is {@code null}
     * @see #putNet(String)
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

    public SOCGame getGame( String gameName ) 
    {
        if (null != inProcServer)
            return inProcServer.getGame( gameName );
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
//                if (Version.versionNumber() == 0)
//                {
//                    throw new IllegalStateException("Packaging error: Cannot determine JSettlers version");
//                }

                inProcServer = new SOCServer(SOCServer.PRACTICE_STRINGPORT, SOCServer.SOC_MAXCONN_DEFAULT, null, null);
                inProcServer.setPriority(5);  // same as in SOCServer.main
                inProcServer.start();
            }
            catch (Throwable th)
            {
//                mainDisplay.showErrorDialog
//                        (client.strings.get("pcli.error.startingpractice") + "\n" + th,  // "Problem starting practice server:"
//                                client.strings.get("base.cancel"));
//
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
                sendVersion(true);

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

    public SOCGameList getGames()
    {
        if (null != inProcServer)
            return inProcServer.getGameList();
        return null;
    }

    /**
     * For practice games, reader thread to get messages from the
     * practice server to be treated and reacted to.
     *<P>
     * Before v2.0.00 this class was {@code SOCPlayerClient.SOCPlayerLocalStringReader}.
     *
     * @see NetReadTask
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
                final MessageHandler handler = client.getMessageHandler();
                handler.init( (SOCFullClient) client);

                while (locl.isConnected())
                {
                    String s = locl.readNext();
                    SOCMessage msg = SOCMessage.toMsg(s);

                    if (msg != null)
                        handler.handle(msg, true);
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
