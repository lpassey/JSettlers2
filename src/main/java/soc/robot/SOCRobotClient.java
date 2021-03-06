/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2021 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012 Paul Bilnoski <paul@bilnoski.net>
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
package soc.robot;

import soc.baseclient.ServerConnectInfo;
import soc.baseclient.SOCDisplaylessPlayerClient;

import soc.communication.Connection;
import soc.disableDebug.D;

import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCPlayer;

import soc.message.*;

import soc.communication.MemServerSocket;

import soc.util.CappedQueue;
import soc.util.CutoffExceededException;
import soc.util.DebugRecorder;
import soc.util.SOCFeatureSet;
import soc.util.SOCRobotParameters;
import soc.util.Version;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import static soc.game.GameState.*;

/**
 * This is a robot client that can play Settlers of Catan.
 *<P>
 * When ready, call {@link #init()} to start the bot's threads and connect to the server.
 * (Built-in bots should set {@link #printedInitialWelcome} beforehand to reduce console clutter.)
 * Once connected, messages from the server are processed in {@link #dispatch(SOCMessage, Connection)}.
 * For each game this robot client plays, there is a {@link SOCRobotBrain}.
 *<P>
 * The built-in robots must be the same version as the server, to simplify things.
 * Third-party bots might be based on this code and be other versions, to simplify their code.
 *<P>
 * The {@link soc.message.SOCImARobot IMAROBOT} connect message gives the bot's class name and
 * a required security cookie, which is passed into the robot client constructor and which must
 * match the server's generated cookie. You can set the server's cookie by setting the
 * server's {@code jsettlers.bots.cookie} parameter, or view it by setting {@code jsettlers.bots.showcookie},
 * when starting the server.
 *<P>
 * Once a bot has connected to the server, it waits to be asked to join games via
 * {@link SOCBotJoinGameRequest BOTJOINREQUEST} messages. When it receives that
 * message type, the bot replies with {@link SOCJoinGame JOINGAME} and the server
 * responds with {@link SOCJoinGameAuth JOINGAMEAUTH}. That message handler creates
 * a {@link SOCRobotBrain} to play the game it is joining.
 *
 *<H4>Debugging</H4>
 * Several bot debug messages are available by sending text messages from other players
 * with certain keywords. See {@link #handleGAMETEXTMSG_debug(SOCGameTextMsg)} for details.
 *
 *<H4>Third-Party Bots</H4>
 * Third-party robot clients can be built from scratch, or extend this class and/or {@link SOCRobotBrain}.
 * If extending this class, please remember to:
 *<UL>
 * <LI> Update {@link #rbclass} value before calling {@link #init()}
 * <LI> Override {@link #createBrain(SOCRobotParameters, SOCGame, CappedQueue)}
 *      to provide your subclass of {@link SOCRobotBrain}
 * <LI> Override {@link #buildClientFeats()} if your bot's optional client features differ from the standard bot
 *</UL>
 * See {@link soc.robot.sample3p.Sample3PClient} for a trivial example subclass.
 *
 *<H4>I18N</H4>
 * The bot ignores the contents of all {@link SOCGameServerText} messages and has no locale.
 * If robot debug commands ({@link SOCGameTextMsg}) are sent to the bot, its responses to the server are in English.
 *
 * @author Robert S Thomas
 */
public class SOCRobotClient extends SOCDisplaylessPlayerClient
{
    /**
     * constants for debug recording
     */
    public static final String CURRENT_PLANS = "CURRENT_PLANS";
    public static final String CURRENT_RESOURCES = "RESOURCES";

    /**
     * For server testing, system property {@code "jsettlers.bots.test.quit_at_joinreq"} to
     * randomly disconnect from the server when asked to join a game. If set, value is
     * the disconnect percentage 0-100.
     * @since 2.0.00
     */
    public static final String PROP_JSETTLERS_BOTS_TEST_QUIT_AT_JOINREQ = "jsettlers.bots.test.quit_at_joinreq";

    /**
     * For debugging feedback, hint text to remind user if debug recorder isn't on.
     * @since 2.0.00
     */
    private static final String HINT_SEND_DEBUG_ON_FIRST = "Debug recorder isn't on. Send :debug-on command first";

    /**
     * For server testing, random disconnect percentage from property
     * {@link #PROP_JSETTLERS_BOTS_TEST_QUIT_AT_JOINREQ}. Defaults to 0.
     * @since 2.0.00
     */
    private static int testQuitAtJoinreqPercent = 0;

    /**
     * For debugging/regression testing, randomly pause responding
     * for several seconds, to simulate a "stuck" robot brain.
     *<P>
     *<b>Note:</b> This debugging tool is not scalable to many simultaneous games,
     * because it delays all messages, not just ones for a specific game / brain,
     * and it won't be our turn in each of those games.
     *<P>
     * Because of the limited scope, currently there is no way to enable this
     * debug flag at runtime; the value must be edited here in the source.
     *
     * @see #DEBUGRANDOMPAUSE_FREQ
     * @see #debugRandomPauseActive
     * @since 1.1.11
     */
    protected static boolean debugRandomPause = false;  // set true to use this debug type

    /**
     * Is {@link #debugRandomPause} currently in effect for this client?
     * If so, this flag becomes {@code true} while pausing.
     * When true, store messages into {@link #debugRandomPauseQueue} instead of
     * sending them to {@link #robotBrains} immediately.
     * The pause goes on until {@link #debugRandomPauseUntil} arrives
     * and then {@code debugRandomPauseActive} becomes {@code false}
     * until the next random float below {@link #DEBUGRANDOMPAUSE_FREQ}.
     * This is all handled within {@link #dispatch(SOCMessage, Connection)}.
     * @since 1.1.11
     */
    protected boolean debugRandomPauseActive = false;

    /**
     * When {@link #debugRandomPauseActive} is true, store incoming messages
     * from the server into this queue until {@link #debugRandomPauseUntil}.
     * Initialized in {@link #dispatch(SOCMessage, Connection)}.
     * @since 1.1.11
     */
    protected Vector<SOCMessage> debugRandomPauseQueue = null;

    /**
     * When {@link #debugRandomPauseActive} is true, resume at this time;
     * same format as {@link System#currentTimeMillis()}.
     * @see #DEBUGRANDOMPAUSE_SECONDS
     * @since 1.1.11
     */
    protected long debugRandomPauseUntil;

    /**
     * When {@link #debugRandomPause} is true but not {@link #debugRandomPauseActive},
     * frequency of activating it; checked for each non-{@link SOCGameTextMsg}
     * and non-{@link SOCGameServerText} message received during our own turn.
     * Default is 0.04 (4%).
     * @since 1.1.11
     */
    protected static final double DEBUGRANDOMPAUSE_FREQ = .04;  // 4%

    /**
     * When {@link #debugRandomPauseActive} is activated, pause this many seconds
     * before continuing. Default is 12.
     * @see #debugRandomPauseUntil
     */
    protected static final int DEBUGRANDOMPAUSE_SECONDS = 12;

    /**
     * Robot class, to be reported to the server when connecting and
     * sending our {@link SOCImARobot} message. Defaults to
     * {@link SOCImARobot#RBCLASS_BUILTIN}: Third-party bots
     * should update this field before calling {@link #init()}.
     * @since 2.0.00
     */
    protected String rbclass = SOCImARobot.RBCLASS_BUILTIN;

    /**
     * Features supported by this built-in JSettlers robot client.
     * Initialized in {@link #init()}.
     * @since 2.0.00
     */
    protected SOCFeatureSet cliFeats;

    // Note: v2.2.00 moved the security cookie field to serverConnectInfo.robotCookie

    /**
     * the current robot parameters for robot brains
     * @see #handleUPDATEROBOTPARAMS(SOCUpdateRobotParams)
     */
    protected SOCRobotParameters currentRobotParameters;

    /**
     * the robot's "brains", 1 for each game this robot is currently playing.
     * @see SOCDisplaylessPlayerClient#games
     */
    protected Hashtable<String, SOCRobotBrain> robotBrains = new Hashtable<>();

    /**
     * the message queues for the different brains
     */
    protected Hashtable<String, CappedQueue<SOCMessage>> brainQs = new Hashtable<>();

    /**
     * a table of requests from the server to sit at games
     */
    private Hashtable<String, Integer> seatRequests = new Hashtable<>();

    /**
     * Options for all games on the server we've been asked to join.
     * Some games may have no options, so will have no entry here,
     * although they will have an entry in {@link #games} once joined.
     * Key = game name.
     *<P>
     * Entries are added in {@link #handleBOTJOINGAMEREQUEST(SOCBotJoinGameRequest)}.
     * Since the robot and server are the same version, the
     * set of "known options" will always be in sync.
     */
    protected Hashtable<String, SOCGameOptionSet> gameOptions = new Hashtable<>();

    /**
     * number of games this bot has played
     */
    protected int gamesPlayed;

    /**
     * number of games finished
     */
    protected int gamesFinished;

    /**
     * number of games this bot has won
     */
    protected int gamesWon;

    /**
     * number of clean brain kills
     */
    protected int cleanBrainKills;

    /**
     * start time
     */
    protected final long startTime;

    /**
     * Have we printed the initial welcome message text from server?
     * Suppress further ones (disconnect-reconnect).
     *<P>
     * Can also set this {@code true} before calling {@link #init()}
     * to avoid printing the initial welcome.
     *
     * @since 1.1.06
     */
    public boolean printedInitialWelcome = false;

    /**
     * Constructor for a robot which will connect to a TCP or local server.
     * Does not actually connect here: Call {@link #init()} when ready.
     *
     * @param sci  Server connect info (TCP or local) with {@code robotCookie}; not {@code null}
     * @param nn nickname for robot
     * @param pw Optional password for robot, or {@code null}
     * @throws IllegalArgumentException if {@code sci == null}
     * @since 2.2.00
     */
    public SOCRobotClient( final ServerConnectInfo sci, final String nn, final String pw )
        throws IllegalArgumentException
    {
        super( sci );

        gamesPlayed = 0;
        gamesFinished = 0;
        gamesWon = 0;
        cleanBrainKills = 0;
        startTime = System.currentTimeMillis();
        nickname = nn;
        password = pw;

        String val = System.getProperty( PROP_JSETTLERS_BOTS_TEST_QUIT_AT_JOINREQ );
        if (val != null)
            try
            {
                testQuitAtJoinreqPercent = Integer.parseInt( val );
            }
            catch (NumberFormatException ignored ) {}
    }

    /**
     * Constructor for a robot which will connect to the specified host, on the specified port.
     * Does not actually connect here: Call {@link #init()} when ready.
     *<P>
     * This deprecated constructor is kept only for compatibility with third-party bot clients.
     *
     * @param h  host
     * @param p  port
     * @param nn nickname for robot
     * @param pw password for robot
     * @param co  cookie for robot connections to server
     * @deprecated In v2.2.00 and newer, use the {@link #SOCRobotClient(ServerConnectInfo, String, String)}
     *     constructor instead:<BR>
     *     {@code new SOCRobotClient(new ServerConnectInfo(h, p, co), nn, pw);}
     */
    public SOCRobotClient( final String h, final int p, final String nn, final String pw, final String co )
    {
        this( new ServerConnectInfo( h, p, co ), nn, pw );
    }

    /**
     * Initialize the robot player; connect to server and send first messages
     * including our version, features from {@link #buildClientFeats()}, and {@link #rbclass}.
     * If fails to connect, sets {@link #ex} and prints it to {@link System#err}.
     */
    public void init()
    {
        try
        {
            connection = MemServerSocket.connectTo( Connection.JVM_STRINGPORT, null, D.ebugIsEnabled() );
            connection.setData( nickname );
            connection.startMessageProcessing( this );
            connected = true;

            if (cliFeats == null)
            {
                cliFeats = buildClientFeats();
                // subclass or third-party bot may override: must check result
                if (cliFeats == null)
                    throw new IllegalStateException( "buildClientFeats() must not return null" );
            }

            connection.send( new SOCVersion( Version.versionNumber(), Version.version(), Version.buildnum(),
                cliFeats.getEncodedList(), null ));
            connection.send( new SOCImARobot( nickname, serverConnectInfo.robotCookie, rbclass ));
        }
        catch( Exception e )
        {
            ex = e;
            System.err.println( "Could not connect to the server: " + ex );
        }
    }

    /**
     * Disconnect and then try to reconnect. Sends the same messages as {@link #init()}.
     * If the reconnect fails, will retry a maximum of 3 times.
     * If those attempts all fail, {@link #connected} will be false and {@link #ex} will be set.
     * Otherwise when method returns, {@link #connected} is true and {@code ex} is null.
     */
    public void disconnectReconnect()
    {
        D.ebugPrintlnINFO( "(*)(*)(*)(*)(*)(*)(*) disconnectReconnect()" );
        ex = null;

        for (int attempt = 3; attempt > 0; --attempt)
        {
            try
            {
                connected = false;
                {
                    connection.disconnect();
                    connection = MemServerSocket.connectTo( Connection.JVM_STRINGPORT, null, D.ebugIsEnabled() );
                    connection.setData( nickname  );
                    connection.startMessageProcessing( this );
                }
                connected = true;

                //resetThread = new SOCRobotResetThread(this);
                //resetThread.start();
                connection.send( new SOCVersion(Version.versionNumber(), Version.version(),
                    Version.buildnum(), cliFeats.getEncodedList(), null));
                connection.send( new SOCImARobot(nickname, serverConnectInfo.robotCookie,
                    SOCImARobot.RBCLASS_BUILTIN));

                break;  // <--- Exit attempt-loop ---
            }
            catch( Exception e )
            {
                ex = e;
                System.err.println( "disconnectReconnect error: " + ex );
                System.err.println( "-> Retrying" );
            }
        }

        if (!connected)
        {
            System.err.println( "-> Giving up" );

            // Couldn't reconnect. Shut down active games' brains.
            for (SOCRobotBrain rb : robotBrains.values())
                rb.kill();
        }
    }

    /**
     * Build the set of optional client features this bot supports, to send to the server.
     * ({@link SOCFeatureSet#CLIENT_6_PLAYERS}, etc.)
     *<P>
     * Third-party subclasses should override this if their features are different.
     *<P>
     * The built-in robots' client features are currently:
     *<UL>
     * <LI>{@link SOCFeatureSet#CLIENT_6_PLAYERS}
     * <LI>{@link SOCFeatureSet#CLIENT_SEA_BOARD}
     * <LI>{@link SOCFeatureSet#CLIENT_SCENARIO_VERSION} = {@link Version#versionNumber()}
     *</UL>
     * For robot debugging and testing, will also add a feature from
     * {@link SOCDisplaylessPlayerClient#PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P} if set,
     * and create that Known Option with {@link SOCGameOption#FLAG_3RD_PARTY} if not already created:
     * Calls {@link SOCGameOptionSet#addKnownOption(SOCGameOption)}.
     *<P>
     * Called from {@link #init()}.
     *
     * @return This bot's set of implemented optional client features, if any, or an empty set (not {@code null})
     * @since 2.0.00
     */
    protected SOCFeatureSet buildClientFeats()
    {
        SOCFeatureSet feats = new SOCFeatureSet( false, false );
        feats.add( SOCFeatureSet.CLIENT_6_PLAYERS );
        feats.add( SOCFeatureSet.CLIENT_SEA_BOARD );
        feats.add( SOCFeatureSet.CLIENT_SCENARIO_VERSION, Version.versionNumber() );

        String gameopt3p = System.getProperty( SOCDisplaylessPlayerClient.PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P );
        if (gameopt3p != null)
        {
            gameopt3p = gameopt3p.toUpperCase( Locale.US );
            feats.add( "com.example.js." + gameopt3p );

            if (null == knownOpts.getKnownOption( gameopt3p, false ))
            {
                knownOpts.addKnownOption( new SOCGameOption
                    ( gameopt3p, 2000, Version.versionNumber(), false,
                        SOCGameOption.FLAG_3RD_PARTY | SOCGameOption.FLAG_DROP_IF_UNUSED,
                        "Client test 3p option " + gameopt3p ) );
                // similar code is in SOCPlayerClient constructor
            }
        }

        return feats;
    }

    /**
     * Factory method for creating a new {@link SOCRobotBrain}.
     *<P>
     * Third-party clients can override this method to use this client with
     * different robot brain subclasses.
     *
     * @param params  the robot parameters to use
     * @param ga  the game in which the brain will play
     * @param mq  the inbound message queue for this brain from the client
     * @return the newly created brain
     * @since 2.0.00
     */
    public SOCRobotBrain createBrain
    ( final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq )
    {
        return new SOCRobotBrain( this, params, ga, mq );
    }

    @Override       // the client side doesn't need to discriminate between first and subsequent messages.
    public void dispatchFirst( SOCMessage message, Connection connection ) throws IllegalStateException
    {
        dispatch( message, connection );
    }

    /**
     * dispatch the incoming messages to the appropriate message handler
     * Messages of unknown type are ignored. All {@link SOCGameServerText} messages are ignored
     * ({@code mes} will be null from {@link SOCMessage#toMsg(String)}).
     *<P>
     *<B>Note:</B> If a message doesn't need any robot-specific handling, and
     * doesn't appear as a specific case in this method's switch, this method
     * calls {@link SOCDisplaylessPlayerClient#dispatch(SOCMessage, Connection)} for it.
     *
     * @param mes A generic SOCMessage to be handled. This method must figure out
     *                   what kind of message it is to dispatch it correctly
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public void dispatch(SOCMessage mes, Connection connection)
    {
        if (mes == null)
            return;  // Message syntax error or unknown type

        // Using debugRandomPause?
        if (debugRandomPause && (!robotBrains.isEmpty())
            && (mes instanceof SOCMessageForGame)
            && !(mes instanceof SOCGameTextMsg)
            && !(mes instanceof SOCGameServerText)
            && !(mes instanceof SOCTurn))
        {
            final String ga = ((SOCMessageForGame) mes).getGameName();
            if (ga != null)
            {
                SOCRobotBrain brain = robotBrains.get( ga );
                if (brain != null)
                {
                    if (!debugRandomPauseActive)
                    {
                        // random chance of doing so
                        if ((Math.random() < DEBUGRANDOMPAUSE_FREQ)
                            && ((debugRandomPauseQueue == null)
                            || (debugRandomPauseQueue.isEmpty())))
                        {
                            SOCGame gm = games.get( ga );
                            final int cpn = gm.getCurrentPlayerNumber();
                            SOCPlayer rpl = gm.getPlayer( nickname );
                            if ((rpl != null) && (cpn == rpl.getPlayerNumber())
                                && (gm.getGameState().gt( STARTS_WAITING_FOR_PICK_GOLD_RESOURCE )))
                            {
                                // we're current player, pause us
                                debugRandomPauseActive = true;
                                debugRandomPauseUntil = System.currentTimeMillis() + (1000L * DEBUGRANDOMPAUSE_SECONDS);
                                if (debugRandomPauseQueue == null)
                                    debugRandomPauseQueue = new Vector<>();
                                System.err.println( "L379 -> do random pause: " + nickname );
                                sendText( gm,
                                    "debugRandomPauseActive for " + DEBUGRANDOMPAUSE_SECONDS + " seconds" );
                            }
                        }
                    }
                }
            }
        }

        if (debugRandomPause && debugRandomPauseActive)
        {
            if ((System.currentTimeMillis() < debugRandomPauseUntil)
                && !(mes instanceof SOCTurn))
            {
                // time hasn't arrived yet, and still our turn:
                //   Add message to queue (even non-game and SOCGameTextMsg)
                debugRandomPauseQueue.addElement( mes );

                return;  // <--- Early return: debugRandomPauseActive ---
            }

            // time to resume the queue
            debugRandomPauseActive = false;
            while (!debugRandomPauseQueue.isEmpty())
            {
                // calling ourself is safe, because
                //  ! queue.isEmpty; thus won't decide
                //  to set debugRandomPauseActive=true again.
                dispatch( debugRandomPauseQueue.firstElement(), null );
                debugRandomPauseQueue.removeElementAt( 0 );
            }
            // Don't return from this method yet,
            // we still need to process mes.
        }

        if (   (debugTraffic || D.ebugIsEnabled())
            && ! (   (mes instanceof SOCServerPing)
                  && (nextServerPingExpectedAt != 0)
            && (Math.abs( System.currentTimeMillis() - nextServerPingExpectedAt ) <= 66000)))
        // within 66 seconds of the expected time; see displaylesscli.handleSERVERPING
        {
            soc.debug.D.ebugPrintlnINFO( "IN - " + nickname + " - " + mes );
        }

        try
        {
            switch (mes.getType())
            {
            /**
             * status message
             */
            case SOCMessage.STATUSMESSAGE:
                handleSTATUSMESSAGE( (SOCStatusMessage) mes );
                break;

            /**
             * admin ping
             */
            case SOCMessage.ADMINPING:
                handleADMINPING( (SOCAdminPing) mes );
                break;

            /**
             * admin reset
             */
            case SOCMessage.ADMINRESET:
                handleADMINRESET( (SOCAdminReset) mes );
                break;

            /**
             * update the current robot parameters
             */
            case SOCMessage.UPDATEROBOTPARAMS:
                handleUPDATEROBOTPARAMS( (SOCUpdateRobotParams) mes );
                break;

            /**
             * join game authorization
             */
            case SOCMessage.JOINGAMEAUTH:
                handleJOINGAMEAUTH((SOCJoinGameAuth) mes );
                break;

            /**
             * game has been destroyed
             */
            case SOCMessage.DELETEGAME:
                handleDELETEGAME( (SOCDeleteGame) mes );
                break;

            /**
             * list of game members
             */
            case SOCMessage.GAMEMEMBERS:
                handleGAMEMEMBERS( (SOCGameMembers) mes );
                break;

            /**
             * game text message (bot debug commands)
             */
            case SOCMessage.GAMETEXTMSG:
                handleGAMETEXTMSG( (SOCGameTextMsg) mes );
                break;

            /**
             * someone is sitting down
             */
            case SOCMessage.SITDOWN:
                handleSITDOWN( (SOCSitDown) mes );
                break;

            /**
             * update the state of the game
             */
            case SOCMessage.GAMESTATE:
                handleGAMESTATE( (SOCGameState) mes );
                break;

            /**
             * a player built something
             */
            case SOCMessage.PUTPIECE:
                handlePUTPIECE( (SOCPutPiece) mes );
                break;

            /**
             * Update game data for bank trade. Added 2021-01-20 for v2.4.50
             */
            case SOCMessage.BANKTRADE:
                if (super.handleBANKTRADE(games, (SOCBankTrade) mes))
                    handlePutBrainQ((SOCMessageForGame) mes);
                break;

            /**
             * the server is requesting that we join a game
             */
            case SOCMessage.BOTJOINGAMEREQUEST:
                SOCBotJoinGameRequest botJoinGameRequestMsg = (SOCBotJoinGameRequest) mes;
                // turn on debug traffic for a specific robot player.
//                connection.setDebugTraffic( botJoinGameRequestMsg.getPlayerNumber() == 0 );
                handleBOTJOINGAMEREQUEST( botJoinGameRequestMsg );
                break;

            /**
             * message that means the server wants us to leave the game
             */
            case SOCMessage.ROBOTDISMISS:
                handleROBOTDISMISS( (SOCRobotDismiss) mes );
                break;

            /**
             * handle board reset (new game with same players, same game name, new layout).
             */
            case SOCMessage.RESETBOARDAUTH:
                handleRESETBOARDAUTH( (SOCResetBoardAuth) mes );
                break;

            /**
             * generic "simple request" responses or announcements from the server.
             * Message type added 2013-02-17 for v1.1.18,
             * bot ignored these until 2015-10-10 for v2.0.00 SC_PIRI
             * and for PROMPT_PICK_RESOURCES from gold hex.
             */
            case SOCMessage.SIMPLEREQUEST:
                handleSIMPLEREQUEST( games, (SOCSimpleRequest) mes );
                handlePutBrainQ( (SOCSimpleRequest) mes );
                break;

            /**
             * generic "simple action" announcements from the server.
             * Added 2013-09-04 for v1.1.19.
             */
            case SOCMessage.SIMPLEACTION:
                handleSIMPLEACTION(games, (SOCSimpleAction) mes);
                handlePutBrainQ( (SOCSimpleAction) mes );
                break;

            /**
             * a special inventory item action: either add or remove,
             * or we cannot play our requested item.
             * Added 2013-11-26 for v2.0.00.
             */
            case SOCMessage.INVENTORYITEMACTION:
            {
                    final boolean isReject = handleINVENTORYITEMACTION
                    ( games, (SOCInventoryItemAction) mes );
                if (isReject)
                    handlePutBrainQ( (SOCInventoryItemAction) mes );
            }
            break;

            /**
             * Special Item change announcements.
             * Added 2014-04-16 for v2.0.00.
             */
            case SOCMessage.SETSPECIALITEM:
                handleSETSPECIALITEM(games, (SOCSetSpecialItem) mes);
                handlePutBrainQ( (SOCSetSpecialItem) mes );
                break;

            // These message types are handled entirely by SOCRobotBrain,
            // which will update game data and do any bot-specific tracking or actions needed:

            case SOCMessage.ACCEPTOFFER:
            case SOCMessage.CANCELBUILDREQUEST:  // current player has cancelled an initial settlement
            case SOCMessage.CHOOSEPLAYER:  // server wants our player to choose to rob cloth or rob resources from victim
            case SOCMessage.CHOOSEPLAYERREQUEST:
            case SOCMessage.CLEAROFFER:
            case SOCMessage.DEVCARDACTION:  // either draw, play, or add to hand, or cannot play our requested dev card
            case SOCMessage.DICERESULT:
            case SOCMessage.DISCARDREQUEST:
            case SOCMessage.MAKEOFFER:
            case SOCMessage.MOVEPIECE:   // move a previously placed ship; will update game data and player trackers
            case SOCMessage.MOVEROBBER:
            case SOCMessage.PLAYERELEMENT:
            case SOCMessage.PLAYERELEMENTS:  // apply multiple PLAYERELEMENT updates; added 2017-12-10 for v2.0.00
            case SOCMessage.REJECTOFFER:
            case SOCMessage.REPORTROBBERY:  // added 2021-01-05 for v2.4.50
            case SOCMessage.RESOURCECOUNT:
            case SOCMessage.STARTGAME:  // added 2017-12-18 for v2.0.00 when gameState became a field of this message
            case SOCMessage.TIMINGPING:  // server's 1x/second timing ping
            case SOCMessage.TURN:
                handlePutBrainQ( (SOCMessageForGame) mes );
                break;

            // These message types are ignored by the robot client;
            // don't send them to SOCDisplaylessClient.treat:

            case SOCMessage.BCASTTEXTMSG:
            case SOCMessage.CHANGEFACE:
            case SOCMessage.CHANNELMEMBERS:
            case SOCMessage.CHANNELS:        // If bot ever uses CHANNELS, update SOCChannels class javadoc
            case SOCMessage.CHANNELTEXTMSG:
            case SOCMessage.DELETECHANNEL:
            case SOCMessage.GAMES:
            case SOCMessage.GAMESERVERTEXT:  // SOCGameServerText contents are ignored by bots
                // (but not SOCGameTextMsg, which is used solely for debug commands)
            case SOCMessage.GAMESTATS:
            case SOCMessage.JOINCHANNEL:
            case SOCMessage.JOINCHANNELAUTH:
            case SOCMessage.LEAVECHANNEL:
            case SOCMessage.NEWCHANNEL:
            case SOCMessage.NEWGAME:
            case SOCMessage.SETSEATLOCK:
                break;  // ignore this message type

            /**
             * Call SOCDisplaylessClient.treat for all other message types.
             * For types relevant to robots, it will update data from the message contents.
             * Other message types will be ignored.
             */
            default:
                treat( mes, true );
            }
        }
        catch( Throwable e )
        {
            System.err.println( "SOCRobotClient treat ERROR - " + e + " " + e.getMessage() );
            e.printStackTrace();
            while (e.getCause() != null)
            {
                e = e.getCause();
                System.err.println( " -> nested: " + e.getClass() );
                e.printStackTrace();
            }
            System.err.println( "-- end stacktrace --" );
            System.out.println( "  For message: " + mes );
        }
    }

    /**
     * handle the admin ping message
     * @param mes  the message
     */
    protected void handleADMINPING( SOCAdminPing mes )
    {
        D.ebugPrintlnINFO( "*** Admin Ping message = " + mes );

        SOCGame ga = games.get( mes.getGameName() );

        //
        //  if the robot hears a PING and is in the game
        //  where the admin is, then just say "OK".
        //  otherwise, join the game that the admin is in
        //
        //  note: this is a hack because the bot never
        //        leaves the game and the game must be
        //        killed by the admin
        //
        if (ga != null)
        {
            sendText( ga, "OK" );
        }
        else
        {
            connection.send( new SOCJoinGame(nickname, password, SOCMessage.EMPTYSTR, mes.getGameName()));
        }
    }

    /**
     * handle the admin reset message
     * @param mes  the message
     */
    protected void handleADMINRESET( SOCAdminReset mes )
    {
        D.ebugPrintlnINFO( "*** Admin Reset message = " + mes );
        disconnectReconnect();
    }

    /**
     * handle the update robot params message
     * @param mes  the message
     */
    protected void handleUPDATEROBOTPARAMS( SOCUpdateRobotParams mes )
    {
        currentRobotParameters = new SOCRobotParameters( mes.getRobotParameters() );

        if (!printedInitialWelcome)
        {
            // Needed only if server didn't send StatusMessage during initial connect.
            // Server won't send status unless its Debug Mode is on.
            System.err.println( "Robot " + getNickname() + ": Authenticated to server." );
            printedInitialWelcome = true;
        }
        if (D.ebugIsEnabled())
            D.ebugPrintlnINFO( "*** current robot parameters = " + currentRobotParameters );
    }

    /**
     * Handle the "version" message, server's version report.
     *<P>
     * Because SOCDisplaylessPlayerClient is used only for the
     * robot, and the robot should always be the same version as
     * the server, don't ask server for info about
     * {@link soc.game.SOCGameOption game option} deltas between
     * the two versions.
     *<P>
     * If somehow the server isn't our version, print an error and disconnect.
     *
     * @param isLocal  Is the server local, or remote?  Client can be connected
     *                only to local, or remote.
     * @param mes  the message
     * @since 1.1.00
     */
    @Override
    protected void handleVERSION(boolean isLocal, SOCVersion mes)
    {
        D.ebugPrintlnINFO( "handleVERSION: " + mes );
        int vers = mes.getVersionNumber();

        connection.setVersion( vers, true );
        final int ourVers = Version.versionNumber();
        if (vers != ourVers)
        {
            final String errmsg =
                "Internal error SOCDisplaylessPlayerClient.handleVERSION: Server must be same as our version "
                    + ourVers + ", not " + vers;  // i18n: Unlikely error, keep un-localized for possible bug reporting
            System.err.println( errmsg );
            ex = new IllegalStateException( errmsg );
            destroy();
        }

        SOCGameOptionGetInfos ogiMsg = new SOCGameOptionGetInfos( null, false, false );

        connection.send( ogiMsg );  // ask for the game options from the server on the same connection it came in on.
    }

    @Override
    protected void handleJOINCHANNEL( SOCJoinChannel mes )
    {

    }

    /**
     * handle the "join game request" message.
     * Remember the game options, and record in {@link #seatRequests}.
     * Send a {@link SOCJoinGame JOINGAME} to server in response.
     * Server will reply with {@link SOCJoinGameAuth JOINGAMEAUTH}.
     *<P>
     * Board resets are handled similarly.
     *<P>
     * In v1.x this method was {@code handleJOINGAMEREQUEST}.
     *
     * @param mes  the message
     *
     * @see #handleRESETBOARDAUTH(SOCResetBoardAuth)
     */
    protected void handleBOTJOINGAMEREQUEST( SOCBotJoinGameRequest mes )
    {
        D.ebugPrintlnINFO( "**** handleBOTJOINGAMEREQUEST ****" );

        final String gaName = mes.getGameName();

        if ((testQuitAtJoinreqPercent != 0) && (new Random().nextInt( 100 ) < testQuitAtJoinreqPercent))
        {
            System.err.println
                ( " -- " + nickname + " leaving at JoinGameRequest('" + gaName + "', " + mes.getPlayerNumber()
                    + "): " + PROP_JSETTLERS_BOTS_TEST_QUIT_AT_JOINREQ );
            connection.send( new SOCLeaveAll() );

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}  // wait for send/receive
            connection.disconnect();
            return;  // <--- Disconnected from server ---
        }

        final Map<String, SOCGameOption> gaOpts = mes.getOptions( knownOpts );
        if (gaOpts != null)
            gameOptions.put( gaName, new SOCGameOptionSet( gaOpts ) );

        seatRequests.put( gaName, mes.getPlayerNumber() );
        connection.send( new SOCJoinGame(nickname, password, SOCMessage.EMPTYSTR, gaName));
        {
            D.ebugPrintlnINFO( "**** sent SOCJoinGame ****" );
        }
    }

    /**
     * handle the "status message" message by printing it to System.err;
     * messages with status value 0 are ignored (no problem is being reported)
     * once the initial welcome message has been printed.
     * Status {@link SOCStatusMessage#SV_SERVER_SHUTDOWN} calls {@link Connection#disconnect()}
     * so as to not print futile reconnect attempts on the terminal.
     * @param mes  the message
     * @since 1.1.00
     */
    @Override
    protected void handleSTATUSMESSAGE( SOCStatusMessage mes )
    {
        int sv = mes.getStatusValue();
        if (sv == SOCStatusMessage.SV_OK_DEBUG_MODE_ON)
            sv = 0;
        else if (sv == SOCStatusMessage.SV_SERVER_SHUTDOWN)
        {
            connection.disconnect();
            return;
        }

        if ((sv != 0) || !printedInitialWelcome)
        {
            System.err.println( "Robot " + getNickname() + ": Status "
                + sv + " from server: " + mes.getStatus() );
            if (sv == 0)
                printedInitialWelcome = true;
        }
    }

    /**
     * handle the "join game authorization" message
     * @param mes  the message
     * @throws IllegalStateException if board size {@link SOCGameOption} "_BHW" isn't defined (unlikely internal error)
     */
    @Override
    protected void handleJOINGAMEAUTH( SOCJoinGameAuth mes )
    {
        gamesPlayed++;

        final String gaName = mes.getGameName();

        final SOCGameOptionSet gameOpts = gameOptions.get( gaName );
        final int bh = mes.getBoardHeight(), bw = mes.getBoardWidth();
        if ((bh != 0) || (bw != 0))
        {
            // Encode board size to pass through game constructor.
            // gameOpts won't be null, because bh, bw are used only with SOCBoardLarge which requires a gameopt
            SOCGameOption opt = knownOpts.getKnownOption( "_BHW", true );
            if (opt == null)
                throw new IllegalStateException( "Internal error: Game opt _BHW not known" );
            opt.setIntValue( (bh << 8) | bw );
            gameOpts.put( opt );
        }

        try
        {
            final SOCGame ga = new SOCGame( gaName, gameOpts, knownOpts );
            ga.serverVersion = connection.getRemoteVersion(); //    (isPractice) ? sLocalVersion : sVersion;
            games.put( gaName, ga );

            CappedQueue<SOCMessage> brainQ = new CappedQueue<>();
            brainQs.put( gaName, brainQ );

            SOCRobotBrain rb = createBrain( currentRobotParameters, ga, brainQ );
            robotBrains.put( gaName, rb );
        }
        catch( IllegalArgumentException e )
        {
            System.err.println
                ( "Sync error: Bot " + nickname + " can't join game " + gaName + ": " + e.getMessage() );
            brainQs.remove( gaName );
            leaveGame( gaName );
        }
    }

    /**
     * handle the "game members" message, which indicates the entire game state has now been sent.
     * If we have a {@link #seatRequests} for this game, request to sit down now: send {@link SOCSitDown}.
     * @param mes  the message
     */
    @Override
    protected void handleGAMEMEMBERS( SOCGameMembers mes )
    {
        /**
         * sit down to play
         */
        Integer pn = seatRequests.get( mes.getGameName() );

        if (pn != null)
        {
            connection.send( new SOCSitDown( mes.getGameName(), SOCMessage.EMPTYSTR, pn, true));
        }
        else
        {
            System.err.println( "** Cannot sit down: Assert failed: null pn for game " + mes.getGameName() );
        }
    }

    /**
     * handle any per-game message that just needs to go into its game's {@link #brainQs}.
     * This includes all messages that the {@link SOCRobotBrain} needs to react to.
     * @param mes  the message
     * @since 2.0.00
     */
    protected void handlePutBrainQ( SOCMessageForGame mes )
    {
        CappedQueue<SOCMessage> brainQ = brainQs.get( mes.getGameName() );

        if (brainQ != null)
        {
            try
            {
                brainQ.put( (SOCMessage) mes );
            }
            catch( CutoffExceededException exc )
            {
                D.ebugPrintlnINFO( "CutoffExceededException" + exc );
            }
        }
    }

    /**
     * Handle the "game text message" message, including
     * debug text messages to the robot which start with
     * the robot's nickname + ":".
     * @param mes  the message
     */
    @Override
    protected void handleGAMETEXTMSG( SOCGameTextMsg mes )
    {
        //D.ebugPrintln(mes.getNickname()+": "+mes.getText());
        if (mes.getText().startsWith( nickname ))
        {
            handleGAMETEXTMSG_debug( mes );
        }
    }

    /**
     * Handle debug text messages from players to the robot, which start with
     * the robot's nickname + ":".
     * @since 1.1.12
     */
    protected void handleGAMETEXTMSG_debug( SOCGameTextMsg mes )
    {
        final int nL = nickname.length();
        try
        {
            if (mes.getText().charAt( nL ) != ':')
                return;
        }
        catch( IndexOutOfBoundsException e )
        {
            return;
        }
        final String gaName = mes.getGameName();
        final String dcmd = mes.getText().substring( nL );

        if (dcmd.startsWith( ":debug-off" ))
        {
            SOCGame ga = games.get( gaName );
            SOCRobotBrain brain = robotBrains.get( gaName );

            if (brain != null)
            {
                brain.turnOffDRecorder();
                sendText( ga, "Debug mode OFF" );
            }
        }

        else if (dcmd.startsWith( ":debug-on" ))
        {
            SOCGame ga = games.get( gaName );
            SOCRobotBrain brain = robotBrains.get( gaName );

            if (brain != null)
            {
                brain.turnOnDRecorder();
                sendText( ga, "Debug mode ON" );
            }
        }

        else if (dcmd.startsWith( ":current-plans" ) || dcmd.startsWith( ":cp" ))
        {
            sendRecordsText( gaName, CURRENT_PLANS, false );
        }

        else if (dcmd.startsWith( ":current-resources" ) || dcmd.startsWith( ":cr" ))
        {
            sendRecordsText( gaName, CURRENT_RESOURCES, false );
        }

        else if (dcmd.startsWith( ":last-plans" ) || dcmd.startsWith( ":lp" ))
        {
            sendRecordsText( gaName, CURRENT_PLANS, true );
        }

        else if (dcmd.startsWith( ":last-resources" ) || dcmd.startsWith( ":lr" ))
        {
            sendRecordsText( gaName, CURRENT_RESOURCES, true );
        }

        else if (dcmd.startsWith( ":last-move" ) || dcmd.startsWith( ":lm" ))
        {
            SOCRobotBrain brain = robotBrains.get( gaName );

            if ((brain != null) && (brain.getOldDRecorder().isOn()))
            {
                SOCPossiblePiece lastMove = brain.getLastMove();

                if (lastMove != null)
                {
                    String key = getKeyForType( lastMove );

                    sendRecordsText( gaName, key, true );
                }
            }
            else
            {
                sendText( games.get( gaName ), HINT_SEND_DEBUG_ON_FIRST );
            }
        }

        else if (dcmd.startsWith( ":consider-move " ) || dcmd.startsWith( ":cm " ))
        {
            String[] tokens = dcmd.split( " " );  // ":consider-move road 154"
            final int L = tokens.length;
            String keytoken = (L > 2) ? tokens[L - 2].trim() : "(missing)",
                lasttoken = (L > 1) ? tokens[L - 1].trim() : "(missing)",
                key = getKeyForToken( lasttoken );


            final SOCGame ga = games.get( gaName );
            if (key == null)
            {
                sendText( ga, "Unknown :consider-move type: " + keytoken );
                return;
            }

            sendRecordsText( gaName, key, true );
        }

        else if (dcmd.startsWith( ":last-target" ) || dcmd.startsWith( ":lt" ))
        {
            SOCRobotBrain brain = robotBrains.get( gaName );

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                SOCPossiblePiece lastTarget = brain.getLastTarget();

                if (lastTarget != null)
                {
                    String key = getKeyForType( lastTarget );

                    sendRecordsText( gaName, key, false );
                }
            }
            else
            {
                sendText( games.get( gaName ), HINT_SEND_DEBUG_ON_FIRST );
            }
        }

        else if (dcmd.startsWith( ":consider-target " ) || dcmd.startsWith( ":ct " ))
        {
            String[] tokens = dcmd.split( " " );  // ":consider-target road 154"
            final int L = tokens.length;
            String keytoken = (L > 2) ? tokens[L - 2].trim() : "(missing)",
                lasttoken = (L > 1) ? tokens[L - 1].trim() : "(missing)",
                key = getKeyForToken( lasttoken );

            final SOCGame ga = games.get( gaName );
            if (key == null)
            {
                sendText( ga, "Unknown :consider-target type: " + keytoken );
                return;
            }

            sendRecordsText( gaName, key, false );
        }

        else if (dcmd.startsWith( ":print-vars" ) || dcmd.startsWith( ":pv" ))
        {
            // "prints" the results as series of SOCGameTextMsg to game
            debugPrintBrainStatus( gaName, true );
        }

        else if (dcmd.startsWith( ":stats" ))
        {
            SOCGame ga = games.get( gaName );
            sendText( ga, "Games played:" + gamesPlayed );
            sendText( ga, "Games finished:" + gamesFinished );
            sendText( ga, "Games won:" + gamesWon );
            sendText( ga, "Clean brain kills:" + cleanBrainKills );
            sendText( ga, "Brains running: " + robotBrains.size() );

            Runtime rt = Runtime.getRuntime();
            sendText( ga, "Total Memory:" + rt.totalMemory() );
            sendText( ga, "Free Memory:" + rt.freeMemory() );
        }

        else if (dcmd.startsWith( ":gc" ))
        {
            SOCGame ga = games.get( gaName );
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            sendText( ga, "Free Memory:" + rt.freeMemory() );
        }
    }

    private String getKeyForToken( String keytoken )
    {
        if (keytoken.equals( "card" ))
            return "DEVCARD";
        if (keytoken.equals( "road" ))
            return "ROAD" + keytoken;
        if (keytoken.equals( "ship" ))
            return "SHIP" + keytoken;
        if (keytoken.equals( "settlement" ))
            return "SETTLEMENT" + keytoken;
        if (keytoken.equals( "city" ))
            return "CITY" + keytoken;
        return null;
    }

    private String getKeyForType( SOCPossiblePiece possiblePiece )
    {
        String key = null;

        switch (possiblePiece.getType())
        {
        case SOCPossiblePiece.CARD:
            key = "DEVCARD";
            break;

        case SOCPossiblePiece.ROAD:
            key = "ROAD" + possiblePiece.getCoordinates();
            break;

        case SOCPossiblePiece.SETTLEMENT:
            key = "SETTLEMENT" + possiblePiece.getCoordinates();
            break;

        case SOCPossiblePiece.CITY:
            key = "CITY" + possiblePiece.getCoordinates();
            break;

        case SOCPossiblePiece.SHIP:
            key = "SHIP" + possiblePiece.getCoordinates();
            break;
        }
        return key;
    }

    /**
     * handle the "someone is sitting down" message
     * @param mes  the message
     */
    @Override
    protected SOCGame handleSITDOWN( SOCSitDown mes )
    {
        final String gaName = mes.getGameName();

        /**
         * tell the game that a player is sitting
         */
        final SOCGame ga = super.handleSITDOWN( mes );
        if (ga == null)
            return null;

        /**
         * let the robot brain find our player object if we sat down
         */
        final int pn = mes.getPlayerNumber();
        if (nickname.equals( mes.getNickname() ))
        {
            SOCRobotBrain brain = robotBrains.get( gaName );

            if (brain.ourPlayerData != null)
            {
                if ((pn == brain.ourPlayerNumber) && nickname.equals( ga.getPlayer( pn ).getName() ))
                    return ga;  // already sitting in this game at this position, OK (can happen during loadgame)

                throw new IllegalStateException
                    ( "bot " + nickname + " game " + gaName
                        + ": got sitdown(pn=" + pn + "), but already sitting at pn=" + brain.ourPlayerNumber );
            }

            /**
             * retrieve the proper face for our strategy
             */
            int faceId;
            if (brain.getRobotParameters().getStrategyType() == SOCRobotDM.SMART_STRATEGY)
            {
                faceId = -1;  // smarter robot face
            }
            else
            {
                faceId = 0;   // default robot face
            }

            brain.setOurPlayerData();
            brain.start();

            /**
             * change our face to the robot face
             */
            connection.send( new SOCChangeFace(ga.getName(), pn, faceId) );
        }
        else
        {
            /**
             * add tracker for player in previously vacant seat
             */
            SOCRobotBrain brain = robotBrains.get( gaName );

            if (brain != null)
                brain.addPlayerTracker( pn );
        }

        return ga;
    }

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    @Override
    protected void handleDELETEGAME( SOCDeleteGame mes )
    {
        String gameName = mes.getGameName();
        SOCRobotBrain brain = robotBrains.get( gameName );

        if (brain != null)
        {
            SOCGame ga = games.get( gameName );

            if (ga != null)
            {
                if (ga.getGameState() == GAME_OVER)
                {
                    gamesFinished++;

                    if (ga.getPlayer( nickname ).getTotalVP() >= ga.vp_winner)
                    {
                        gamesWon++;
                        // TODO: should check actual winning player number (getCurrentPlayerNumber?)
                    }
                }

                brain.kill();
                robotBrains.remove( gameName );
                brainQs.remove( gameName );
                games.remove( gameName );
            }
        }
    }

    /**
     * Handle the "game state" message; instead of immediately updating state,
     * calls {@link #handlePutBrainQ(SOCMessageForGame)}.
     * @param mes  the message
     */
    @Override
    protected void handleGAMESTATE( SOCGameState mes )
    {
        SOCGame ga = games.get( mes.getGameName() );

        if (ga != null)
        {
            handlePutBrainQ( mes );
        }
    }

    /**
     * handle the "put piece" message
     * @param mes  the message
     */
    protected void handlePUTPIECE( SOCPutPiece mes )
    {
        CappedQueue<SOCMessage> brainQ = brainQs.get( mes.getGameName() );

        if (brainQ != null)
        {
            try
            {
                brainQ.put( mes );
            }
            catch( CutoffExceededException exc )
            {
                D.ebugPrintlnINFO( "CutoffExceededException" + exc );
            }
        }
    }

    /**
     * handle the "dismiss robot" message
     * @param mes  the message
     */
    protected void handleROBOTDISMISS( SOCRobotDismiss mes )
    {
        String gameName = mes.getGameName();
        SOCGame ga = games.get( gameName );
        CappedQueue<SOCMessage> brainQ = brainQs.get( gameName );

        if ((ga != null) && (brainQ != null))
        {
            try
            {
                brainQ.put( mes );
            }
            catch( CutoffExceededException exc )
            {
                D.ebugPrintlnINFO( "CutoffExceededException" + exc );
            }

            /**
             * if the brain isn't alive, then we need to leave
             * the game here, instead of having the brain leave it
             */
            SOCRobotBrain brain = robotBrains.get( gameName );

            if ((brain == null) || (!brain.isAlive()))
            {
                leaveGame( games.get( gameName ), "brain not alive in handleROBOTDISMISS",
                    true, false );
            }
        }
    }

    /**
     * handle board reset
     * (new game with same players, same game name).
     * Destroy old Game object.
     * Unlike <tt>SOCDisplaylessPlayerClient.handleRESETBOARDAUTH</tt>, don't call {@link SOCGame#resetAsCopy()}.
     *<P>
     * Take robotbrain out of old game, don't yet put it in new game.
     * Let server know we've done so, by sending LEAVEGAME via {@link #leaveGame(SOCGame, String, boolean, boolean)}.
     * Server will soon send a BOTJOINGAMEREQUEST if we should join the new game.
     *
     * @param mes  the message
     *
     * @see soc.server.SOCServer#resetBoardAndNotify(String, int)
     * @see #handleBOTJOINGAMEREQUEST(SOCBotJoinGameRequest)
     * @since 1.1.00
     */
    @Override
    protected void handleRESETBOARDAUTH( SOCResetBoardAuth mes )
    {
        D.ebugPrintlnINFO( "**** handleRESETBOARDAUTH ****" );

        String gname = mes.getGameName();
        SOCGame ga = games.get( gname );
        if (ga == null)
            return;  // Not one of our games

        SOCRobotBrain brain = robotBrains.get( gname );
        if (brain != null)
            brain.kill();
        leaveGame( ga, "resetboardauth", false, false );  // Same as in handleROBOTDISMISS
        ga.destroyGame();
    }


    /**
     * Call sendText on each string element of a record from
     * {@link SOCRobotBrain#getDRecorder()} or {@link SOCRobotBrain#getOldDRecorder()}.
     * If no records found or ! {@link DebugRecorder#isOn()}, sends text to let the user know.
     *
     * @param gaName  Game name; if no brain found for game, does nothing
     * @param key  Recorder key for strings to send; not {@code null}
     * @param oldNotCurrent  True if should use {@link SOCRobotBrain#getOldDRecorder()}
     *     instead of {@link SOCRobotBrain#getDRecorder() getDRecorder()}
     * @since 1.1.00
     */
    protected void sendRecordsText
    ( final String gaName, final String key, final boolean oldNotCurrent )
    {
        final SOCRobotBrain brain = robotBrains.get( gaName );
        if (brain == null)
            return;

        final SOCGame ga = games.get( gaName );

        final DebugRecorder recorder = (oldNotCurrent) ? brain.getOldDRecorder() : brain.getDRecorder();
        if (!recorder.isOn())
        {
            sendText( ga, HINT_SEND_DEBUG_ON_FIRST );
            return;
        }

        final List<String> record = recorder.getRecord( key );

        if (record != null)
            for (String str : record)
                sendText( ga, str );
        else
            sendText( ga, "No debug records for " + key );
    }

    /**
     * Print brain variables and status for this game, to {@link System#err}
     * or as {@link SOCGameTextMsg} sent to the game's members,
     * by calling {@link SOCRobotBrain#debugPrintBrainStatus()}.
     * @param gameName  Game name; if no brain for that game, do nothing.
     * @param sendTextToGame  Send to game as {@link SOCGameTextMsg} if true,
     *     otherwise print to {@link System#err}.
     * @since 1.1.13
     */
    public void debugPrintBrainStatus( String gameName, final boolean sendTextToGame )
    {
        SOCRobotBrain brain = robotBrains.get( gameName );
        if (brain == null)
            return;

        List<String> rbSta = brain.debugPrintBrainStatus();
        if (sendTextToGame)
            for (final String st : rbSta)
                connection.send( new SOCGameTextMsg(gameName, nickname, st) );
        else
            for (final String st : rbSta)
                System.err.println( st );
    }

    /**
     * the user leaves the given game
     *
     * @param ga   the game
     * @param leaveReason reason for leaving
     * @param showReason  If true print bot, game, and {@code leaveReason} even if not {@link D#ebugIsEnabled()}
     * @param showDebugTrace  If true print current thread's stack trace
     */
    public void leaveGame
    ( final SOCGame ga, final String leaveReason, final boolean showReason, final boolean showDebugTrace )
    {
        if (ga == null)
            return;

        final String gaName = ga.getName();

        robotBrains.remove( gaName );
        brainQs.remove( gaName );
        games.remove( gaName );

        final String r = (showReason || D.ebugIsEnabled())
            ? ("L1833 robot " + nickname + " leaving game " + gaName + " due to " + leaveReason)
            : null;
        if (showReason)
            soc.debug.D.ebugPrintlnINFO( r );
        else if (r != null)
            D.ebugPrintlnINFO( r );

        if (showDebugTrace)
        {
            soc.debug.D.ebugPrintStackTrace( null, "Leaving game here" );
            System.err.flush();
        }

        connection.send(new SOCLeaveGame( gaName,  nickname, "-" ));
    }

    /**
     * add one to the number of clean brain kills
     */
    public void addCleanKill()
    {
        cleanBrainKills++;
    }

    /**
     * Connection to server has raised an error; leave all games, then try to reconnect.
     */
    @Override
    public void destroy()
    {
        SOCLeaveAll leaveAllMes = new SOCLeaveAll();
        connection.send( leaveAllMes );
        disconnectReconnect();
        if (ex != null)
            System.err.println( "Reconnect to server failed: " + ex );
    }

    /**
     * for stand-alones
     */
    public static void main( String[] args )
    {
        if (args.length < 5)
        {
            System.err.println( "Java Settlers robotclient " + Version.version() +
                ", build " + Version.buildnum() );
            System.err.println( "usage: java soc.robot.SOCRobotClient host port_number bot_nickname password cookie" );
            return;
        }

        SOCRobotClient ex1 = new SOCRobotClient
            ( new ServerConnectInfo( args[0], Integer.parseInt( args[1] ), args[4] ), args[2], args[3] );
        ex1.init();
    }
}
