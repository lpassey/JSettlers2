/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2023 Jeremy D Monin <jeremy@nand.net>
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

import soc.baseclient.MessageHandler;
import soc.disableDebug.D;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCPlayer;
import soc.message.*;
import soc.util.CappedQueue;
import soc.util.CutoffExceededException;
import soc.util.SOCRobotParameters;

import java.util.Map;
import java.util.Random;
import java.util.Vector;

import static soc.robot.SOCRobotClient.*;

public class RobotMessageHandler extends MessageHandler
{
    RobotMessageHandler( SOCRobotClient client )
    {
        super( client );
    }
    
    private SOCRobotClient getClient()
    {
        return (SOCRobotClient) client;
    }
    
    /**
     * Handle the incoming messages.
     * Messages of unknown type are ignored. All {@link SOCGameServerText} messages are ignored.
     * ({@code mes} will be null from {@link SOCMessage#toMsg(String)}).
     *<P>
     *<B>Note:</B> If a message doesn't need any robot-specific handling,
     * and doesn't appear as a specific case in this method's switch,
     * this method calls {@link MessageHandler#handle(SOCMessage)} for it.
     *
     * @param mes    the message
     */
    @Override
    public void handle( SOCMessage mes, boolean didDebugPrintAlready  )
    {
        if (mes == null)
            return;  // Message syntax error or unknown type
        
        SOCRobotClient robotClient = getClient();
        // Using debugRandomPause?
        if (debugRandomPause && (! robotClient.robotBrains.isEmpty())
                && (mes instanceof SOCMessageForGame)
                && ! (mes instanceof SOCGameTextMsg)
                && ! (mes instanceof SOCGameServerText)
                && ! (mes instanceof SOCTurn))
        {
            final String gameName = ((SOCMessageForGame) mes).getGameName();
            if (gameName != null)
            {
                SOCRobotBrain brain = robotClient.robotBrains.get(gameName);
                if (brain != null)
                {
                    if (! robotClient.debugRandomPauseActive)
                    {
                        // random chance of doing so
                        if ((Math.random() < DEBUGRANDOMPAUSE_FREQ)
                                && ((robotClient.debugRandomPauseQueue == null)
                                || (robotClient.debugRandomPauseQueue.isEmpty())))
                        {
                            SOCGame gm = robotClient.getGame( gameName );
                            final int cpn = gm.getCurrentPlayerNumber();
                            SOCPlayer rpl = gm.getPlayer( robotClient.getNickname());
                            if ((rpl != null) && (cpn == rpl.getPlayerNumber())
                                    && (gm.getGameState() >= SOCGame.ROLL_OR_CARD))
                            {
                                // we're current player, pause us
                                robotClient.debugRandomPauseActive = true;
                                robotClient.debugRandomPauseUntil = System.currentTimeMillis() + (1000L * DEBUGRANDOMPAUSE_SECONDS);
                                if (robotClient.debugRandomPauseQueue == null)
                                    robotClient.debugRandomPauseQueue = new Vector<SOCMessage>();
                                System.err.println("L379 -> do random pause: " + robotClient.getNickname());
                                robotClient.sendText( gm,"debugRandomPauseActive for " + DEBUGRANDOMPAUSE_SECONDS + " seconds");
                            }
                        }
                    }
                }
            }
        }
        
        if (debugRandomPause && robotClient.debugRandomPauseActive)
        {
            if ((System.currentTimeMillis() < robotClient.debugRandomPauseUntil)
                    && ! (mes instanceof SOCTurn))
            {
                // time hasn't arrived yet, and still our turn:
                //   Add message to queue (even non-game and SOCGameTextMsg)
                robotClient.debugRandomPauseQueue.addElement(mes);
                
                return;  // <--- Early return: debugRandomPauseActive ---
            }
            
            // time to resume the queue
            robotClient.debugRandomPauseActive = false;
            while (! robotClient.debugRandomPauseQueue.isEmpty())
            {
                // calling ourself is safe, because
                //  ! queue.isEmpty; thus won't decide
                //  to set debugRandomPauseActive=true again.
                handle(robotClient.debugRandomPauseQueue.firstElement(), didDebugPrintAlready );
                robotClient.debugRandomPauseQueue.removeElementAt(0);
            }
            
            // Don't return from this method yet,
            // we still need to process mes.
        }
        
        if (   (debugTraffic || D.ebugIsEnabled())
            && !(   (mes instanceof SOCServerPing)
//                 && (nextServerPingExpectedAt != 0)
//            && (Math.abs(System.currentTimeMillis() - nextServerPingExpectedAt) <= 66000))
        ))
        // within 66 seconds of the expected time; see displaylesscli.handleSERVERPING
        {
            soc.debug.D.ebugPrintlnINFO("IN - " + robotClient.getNickname() + " - " + mes);
        }
        didDebugPrintAlready = true;

        try
        {
            switch (mes.getType())
            {
            /**
             * status message
             */
            case SOCMessage.STATUSMESSAGE:
                // overridden
                handleSTATUSMESSAGE((SOCStatusMessage) mes);
                break;
            
            /**
             * admin ping
             */
            case SOCMessage.ADMINPING:
                handleADMINPING((SOCAdminPing) mes);
                break;
            
            /**
             * admin reset
             */
            case SOCMessage.ADMINRESET:
                handleADMINRESET((SOCAdminReset) mes);
                break;
            
            /**
             * update the current robot parameters
             */
            case SOCMessage.UPDATEROBOTPARAMS:
                handleUPDATEROBOTPARAMS((SOCUpdateRobotParams) mes);
                break;
            
            /**
             * join game authorization
             * not handled by the base class, but similar to PlayerMessageHandler
             */
            case SOCMessage.JOINGAMEAUTH:
                handleJOINGAMEAUTH((SOCJoinGameAuth) mes);
                break;
            
            /**
             * game has been destroyed
             * not handled by the base class, but similar to PlayerMessageHandler
             */
            case SOCMessage.DELETEGAME:
                handleDELETEGAME((SOCDeleteGame) mes);
                break;
            
            /**
             * list of game members
             * not handled by the base class, but similar to PlayerMessageHandler
             */
            case SOCMessage.GAMEMEMBERS:
                handleGAMEMEMBERS((SOCGameMembers) mes);
                break;
            
            /**
             * game text message (bot debug commands)
             * not handled by the base class, but similar to PlayerMessageHandler
             */
            case SOCMessage.GAMETEXTMSG:
                handleGAMETEXTMSG((SOCGameTextMsg) mes);
                break;
            
            /**
             * someone is sitting down
             */
            case SOCMessage.SITDOWN:
                // overridden here
                handleSITDOWN((SOCSitDown) mes);
                break;
            
            /**
             * update the state of the game
             */
            case SOCMessage.GAMESTATE:
                // overridden here
                handleGAMESTATE((SOCGameState) mes);
                break;

            /**
             * the server is requesting that we join a game
             */
            case SOCMessage.BOTJOINGAMEREQUEST:
                handleBOTJOINGAMEREQUEST((SOCBotJoinGameRequest) mes);
                break;
            
            /**
             * message that means the server wants us to leave the game
             */
            case SOCMessage.ROBOTDISMISS:
                handleROBOTDISMISS((SOCRobotDismiss) mes);
                break;
            
            /**
             * handle board reset (new game with same players, same game name, new layout).
             */
            case SOCMessage.RESETBOARDAUTH:
                // overridden
                handleRESETBOARDAUTH((SOCResetBoardAuth) mes);
                break;

            /**
             * {@link #handlePutBrainQ( SOCMessage) does nothing more than place the message
             * into a FIFO queue, where the "brain" for this robot extracts them in order, and
             * acts upon them appropriately. It's similar to this method, but the robot handles
             * them in a separate thread.
             */

            /**
             * a special inventory item action: either add or remove,
             * or we cannot play our requested item.
             * Added 2013-11-26 for v2.0.00.
             */
            case SOCMessage.INVENTORYITEMACTION:
            {
                SOCGame game = robotClient.getGame( ((SOCInventoryItemAction) mes).getGameName());
                final boolean isRejected = staticHandleINVENTORYITEMACTION((SOCInventoryItemAction) mes, game );
                if (isRejected)
                    handlePutBrainQ( (SOCInventoryItemAction) mes );
            }
            break;
            
            /**
             * Special Item change announcements.
             * Added 2014-04-16 for v2.0.00.
             */
            case SOCMessage.SETSPECIALITEM:
                SOCGame game = robotClient.getGame( ((SOCSetSpecialItem) mes).getGameName() );
                staticHandleSETSPECIALITEM( (SOCSetSpecialItem) mes, game );      // TODO: This should not be a static method
                handlePutBrainQ((SOCSetSpecialItem) mes);
                break;
            
            // These message types are handled entirely by SOCRobotBrain,
            // which will update game data and do any bot-specific tracking or actions needed:
            case SOCMessage.ACCEPTOFFER:
            case SOCMessage.BANKTRADE:     // added 2021-01-20 for v2.5.00
            case SOCMessage.BOTGAMEDATACHECK:    // added 2021-09-30 for v2.5.00
            case SOCMessage.CANCELBUILDREQUEST:  // current player has cancelled an initial settlement
            case SOCMessage.CHOOSEPLAYER:  // server wants our player to choose to rob cloth or rob resources from victim
            case SOCMessage.CHOOSEPLAYERREQUEST:
            case SOCMessage.CLEAROFFER:
            case SOCMessage.DECLINEPLAYERREQUEST:
            case SOCMessage.DEVCARDACTION:  // either draw, play, or add to hand, or cannot play our requested dev card
            case SOCMessage.DICERESULT:
            case SOCMessage.DICERESULTRESOURCES:
            case SOCMessage.DISCARD:        // added 2021-11-26 for v2.5.00
            case SOCMessage.DISCARDREQUEST:
            case SOCMessage.GAMESTATS:
            case SOCMessage.MAKEOFFER:
            case SOCMessage.MOVEPIECE:   // move a previously placed ship; will update game data and player trackers
            case SOCMessage.MOVEROBBER:
            case SOCMessage.PLAYERELEMENT:
            case SOCMessage.PLAYERELEMENTS:  // apply multiple PLAYERELEMENT updates; added 2017-12-10 for v2.0.00
            case SOCMessage.PUTPIECE:
            case SOCMessage.REJECTOFFER:
            case SOCMessage.RESOURCECOUNT:
            case SOCMessage.ROBBERYRESULT:  // added 2021-01-05 for v2.5.00
            case SOCMessage.SIMPLEACTION:   // added 2013-09-04 for v1.1.19
            case SOCMessage.SIMPLEREQUEST:  // bot ignored these until 2015-10-10 for v2.0.00
            case SOCMessage.STARTGAME:  // added 2017-12-18 for v2.0.00 when gameState became a field of this message
            case SOCMessage.TIMINGPING:  // server's 1x/second timing ping
            case SOCMessage.TURN:
            case SOCMessage.UNDOPUTPIECE:   // added 2022-11-14 for v2.7.00
                handlePutBrainQ((SOCMessageForGame) mes);
                break;
            
            /**
             *  These message types are ignored by the robot client;
             *  don't send them to {@link MessageHandler#handle
             */
            
            case SOCMessage.BCASTTEXTMSG:
            case SOCMessage.CHANGEFACE:
            case SOCMessage.CHANNELMEMBERS:
            case SOCMessage.CHANNELS:        // If bot ever uses CHANNELS, update SOCChannels class javadoc
            case SOCMessage.CHANNELTEXTMSG:
            case SOCMessage.DELETECHANNEL:
            case SOCMessage.GAMES:
            case SOCMessage.GAMESERVERTEXT:  // SOCGameServerText contents are ignored by bots
                // (but not SOCGameTextMsg, which is used solely for debug commands)
            case SOCMessage.JOINCHANNEL:
            case SOCMessage.JOINCHANNELAUTH:
            case SOCMessage.LEAVECHANNEL:
            case SOCMessage.NEWCHANNEL:
            case SOCMessage.NEWGAME:
            case SOCMessage.SETSEATLOCK:
                break;  // ignore these message types
            
            /**
             * Call {@link MessageHandler#handle( SOCMessage, boolean )} for all other message types.
             * For types relevant to robots, updates game data from the message contents.
             */
            default:
                super.handle( mes , didDebugPrintAlready );
            }
        }
        catch (Throwable e)
        {
            System.err.println("SOCRobotClient handle ERROR - " + e + " " + e.getMessage());
            e.printStackTrace();
            while (e.getCause() != null)
            {
                e = e.getCause();
                System.err.println(" -> nested: " + e.getClass());
                e.printStackTrace();
            }
            System.err.println("-- end stacktrace --");
            System.out.println("  For message: " + mes);
        }
    }

    /**
     * handle the admin ping message
     * @param mes  the message
     */
    protected void handleADMINPING(SOCAdminPing mes)
    {
        D.ebugPrintlnINFO("*** Admin Ping message = " + mes);

        SOCRobotClient robotClient = getClient();

        SOCGame ga = robotClient.getGame( mes.getGameName() );

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
            robotClient.sendText(ga, "OK");
        }
        else
        {
            robotClient.put(SOCJoinGame.toCmd( robotClient.getNickname(), robotClient.getPassword(), SOCMessage.EMPTYSTR, mes.getGameName()));
        }
    }

    /**
     * handle the admin reset message
     * @param mes  the message
     */
    protected void handleADMINRESET(SOCAdminReset mes)
    {
        D.ebugPrintlnINFO("*** Admin Reset message = " + mes);
        getClient().disconnectReconnect();
    }

    /**
     * handle the update robot params message
     * @param mes  the message
     */
    protected void handleUPDATEROBOTPARAMS(SOCUpdateRobotParams mes)
    {
        getClient().currentRobotParameters = new SOCRobotParameters(mes.getRobotParameters());

        if (! getClient().printedInitialWelcome)
        {
            // Needed only if server didn't send StatusMessage during initial connect.
            // Server won't send status unless its Debug Mode is on.
            System.err.println("Robot " + getClient().getNickname() + ": Authenticated to server.");
            getClient().printedInitialWelcome = true;
        }
        if (D.ebugIsEnabled())
            D.ebugPrintlnINFO("*** current robot parameters = " + getClient().currentRobotParameters);
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
    protected void handleBOTJOINGAMEREQUEST(SOCBotJoinGameRequest mes)
    {
        D.ebugPrintlnINFO("**** handleBOTJOINGAMEREQUEST ****");

        final String gaName = mes.getGameName();

        if (   (getClient().testQuitAtJoinreqPercent != 0)
            && (new Random().nextInt(100) < testQuitAtJoinreqPercent))
        {
            System.err.println( " -- "
                    + getClient().getNickname() + " leaving at JoinGameRequest('" + gaName + "', "
                    + mes.getPlayerNumber() + "): " + PROP_JSETTLERS_BOTS_TEST_QUIT_AT_JOINREQ);
            getClient().put(new SOCLeaveAll().toCmd());

            try { Thread.sleep(200); } catch (InterruptedException ignore) {}  // wait for send/receive
            getClient().disconnect();
            return;  // <--- Disconnected from server ---
        }

        final Map<String,SOCGameOption> gaOpts = mes.getOptions(getClient().knownOpts);
        if (gaOpts != null)
            getClient().gameOptions.put(gaName, new SOCGameOptionSet(gaOpts));

        getClient().seatRequests.put(gaName, Integer.valueOf(mes.getPlayerNumber()));
        if (getClient().put(SOCJoinGame.toCmd(
                getClient().getNickname(), getClient().getPassword(), SOCMessage.EMPTYSTR, gaName)))
        {
            D.ebugPrintlnINFO("**** sent SOCJoinGame ****");
        }
    }

    /**
     * handle the "status message" message by printing it to System.err;
     * messages with status value 0 are ignored (no problem is being reported)
     * once the initial welcome message has been printed.
     * Status {@link SOCStatusMessage#SV_SERVER_SHUTDOWN} calls {@link SOCRobotClient#disconnect()}
     * so as not to print futile reconnect attempts on the terminal.
     * @param mes  the message
     * @since 1.1.00
     */
    @Override
    protected void handleSTATUSMESSAGE(SOCStatusMessage mes)
    {
        SOCRobotClient robotClient = getClient();
        int sv = mes.getStatusValue();
        if (sv == SOCStatusMessage.SV_OK_DEBUG_MODE_ON)
            sv = 0;
        else if (sv == SOCStatusMessage.SV_SERVER_SHUTDOWN)
        {
            robotClient.disconnect();
            return;
        }
        
        if ((sv != 0) || ! robotClient.printedInitialWelcome)
        {
            System.err.println("Robot " + getClient().getNickname() + ": Status "
                                       + sv + " from server: " + mes.getStatus());
            if (sv == 0)
                robotClient.printedInitialWelcome = true;
        }
    }
    
    /**
     * handle the "join game authorization" message. Ignored by the base class.
     * @param mes  the message
     * @ param isPractice Is the server local for practice, or remote?
     * TODO: Actually, the server will always be in-process, but the game may not be
     *             a practice game. Does it make any difference?
     * @throws IllegalStateException if board size {@link SOCGameOption} "_BHW" isn't defined (unlikely internal error)
     */
    protected void handleJOINGAMEAUTH(SOCJoinGameAuth mes)
    {
        SOCRobotClient robotClient = getClient();
        boolean isPractice =  robotClient.isLocal();
        robotClient.gamesPlayed++;
        
        final String gaName = mes.getGameName();
        
        final SOCGameOptionSet gameOpts = robotClient.gameOptions.get( gaName);
        final int bh = mes.getBoardHeight(), bw = mes.getBoardWidth();
        if ((bh != 0) || (bw != 0))
        {
            // Encode board size to pass through game constructor.
            // gameOpts won't be null, because bh, bw are used only with SOCBoardLarge which requires a gameopt
            SOCGameOption opt = robotClient.knownOpts.getKnownOption("_BHW", true);
            if (opt == null)
                throw new IllegalStateException("Internal error: Game opt _BHW not known");
            opt.setIntValue((bh << 8) | bw);
            gameOpts.put(opt);
        }
        
        try
        {
            final SOCGame game = new SOCGame( gaName, gameOpts, robotClient.knownOpts );
            game.isPractice = isPractice;
            // TODO: Not sure about this...
            game.serverVersion = getClient().getServerVersion( game ); // ? robotClient.sLocalVersion : robotClient.sVersion;
            robotClient.addGame( game );    // Calls base client version??
            robotClient.putBrain( game );
        }
        catch (IllegalArgumentException e)
        {
            System.err.println( "Sync error: Bot " + robotClient.getNickname() + " can't join game "
                    + gaName + ": " + e.getMessage());
            robotClient.removeBrain( gaName );       // also removes the brain from the brain queue
            robotClient.leaveGame(gaName);
        }
    }
    
    /**
     * handle the "game members" message, which indicates the entire game state has now been sent.
     * If we have a {@link SOCRobotClient#seatRequests} for this game, request to sit down now;
     * send {@link SOCSitDown}. Handled by Swing message handlers, but not by the base class.
     * @param mes  the message
     */
    protected void handleGAMEMEMBERS(SOCGameMembers mes)
    {
        /**
         * sit down to play
         */
        Integer pn = (getClient()).seatRequests.get( mes.getGameName());
        
        /*
        try
        {
            wait(Math.round(Math.random()*1000));
        }
        catch (Exception ignore)
        {
            ;
        }
        */
        if (pn != null)
        {
            getClient().put( SOCSitDown.toCmd( mes.getGameName(), SOCMessage.EMPTYSTR, pn, true) );
        }
        else
        {
            System.err.println("** Cannot sit down: Assert failed: null pn for game " + mes.getGameName());
        }
    }
    /**
     * handle any per-game message that just needs to go into its game's {@link #brainQs}.
     * This includes all messages that the {@link SOCRobotBrain} needs to react to.
     * @param mes  the message
     * @since 2.0.00
     */
    protected void handlePutBrainQ(SOCMessageForGame mes)
    {
        CappedQueue<SOCMessage> brainQ = getClient().brainQs.get(mes.getGameName());

        if (brainQ != null)
        {
            try
            {
                brainQ.put((SOCMessage)mes);
            }
            catch (CutoffExceededException exc)
            {
                D.ebugPrintlnINFO("CutoffExceededException" + exc);
            }
        }
    }

    /**
     * Handle the "game text message" message, including
     * debug text messages to the robot which start with
     * the robot's nickname + ":".
     * @param mes  the message
     */
    protected void handleGAMETEXTMSG(SOCGameTextMsg mes)
    {
        //D.ebugPrintln(mes.getNickname()+": "+mes.getText());
        SOCRobotClient robotClient = getClient();
        if (mes.getText().startsWith( robotClient.getNickname()))
        {
//            handleGAMETEXTMSG_debug(mes);
//        }
//    }
//
            /**
             * Handle debug text messages from players to the robot, which start with
             * the robot's nickname + ":".
             * @since 1.1.12
             */
//    protected void handleGAMETEXTMSG_debug(SOCGameTextMsg mes)
//    {
            final int nL = robotClient.getNickname().length();
            try
            {
                if (mes.getText().charAt( nL ) != ':')
                    return;
            }
            catch (IndexOutOfBoundsException e)
            {
                return;
            }
            final String gaName = mes.getGameName();
            final String dcmd = mes.getText().substring( nL );
            
            SOCGame game = robotClient.getGame( gaName );
            SOCRobotBrain brain = robotClient.robotBrains.get( gaName );
            if (dcmd.startsWith( ":debug-off" ))
            {
                if (brain != null)
                {
                    brain.turnOffDRecorder();
                    robotClient.sendText( game, "Debug mode OFF" );
                }
            }
            
            else if (dcmd.startsWith( ":debug-on" ))
            {
                if (brain != null)
                {
                    brain.turnOnDRecorder();
                    robotClient.sendText( game, "Debug mode ON" );
                }
            }
            
            else if (dcmd.startsWith( ":current-plans" ) || dcmd.startsWith( ":cp" ))
            {
                robotClient.sendRecordsText( gaName, CURRENT_PLANS, false );
            }
            
            else if (dcmd.startsWith( ":current-resources" ) || dcmd.startsWith( ":cr" ))
            {
                robotClient.sendRecordsText( gaName, CURRENT_RESOURCES, false );
            }
            
            else if (dcmd.startsWith( ":last-plans" ) || dcmd.startsWith( ":lp" ))
            {
                robotClient.sendRecordsText( gaName, CURRENT_PLANS, true );
            }
            
            else if (dcmd.startsWith( ":last-resources" ) || dcmd.startsWith( ":lr" ))
            {
                robotClient.sendRecordsText( gaName, CURRENT_RESOURCES, true );
            }
            
            else if (dcmd.startsWith( ":last-move" ) || dcmd.startsWith( ":lm" ))
            {
                if ((brain != null) && (brain.getOldDRecorder().isOn()))
                {
                    SOCPossiblePiece lastMove = brain.getLastMove();
                    
                    if (lastMove != null)
                    {
                        String key = null;
                        
                        switch (lastMove.getType())
                        {
                        case SOCPossiblePiece.CARD:
                            key = "DEVCARD";
                            break;
                        
                        case SOCPossiblePiece.ROAD:
                            key = "ROAD" + lastMove.getCoordinates();
                            break;
                        
                        case SOCPossiblePiece.SETTLEMENT:
                            key = "SETTLEMENT" + lastMove.getCoordinates();
                            break;
                        
                        case SOCPossiblePiece.CITY:
                            key = "CITY" + lastMove.getCoordinates();
                            break;
                        
                        case SOCPossiblePiece.SHIP:
                            key = "SHIP" + lastMove.getCoordinates();
                            break;
                        }
                        
                        robotClient.sendRecordsText( gaName, key, true );
                    }
                }
                else
                {
                    robotClient.sendText( game, HINT_SEND_DEBUG_ON_FIRST );
                }
            }
            
            else if (dcmd.startsWith( ":consider-move " ) || dcmd.startsWith( ":cm " ))
            {
                String[] tokens = dcmd.split( " " );  // ":consider-move road 154"
                final int L = tokens.length;
                String keytoken = (L > 2) ? tokens[L - 2].trim() : "(missing)",
                        lasttoken = (L > 1) ? tokens[L - 1].trim() : "(missing)",
                        key = null;
                
                if (lasttoken.equals( "card" ))
                    key = "DEVCARD";
                else if (keytoken.equals( "road" ))
                    key = "ROAD" + lasttoken;
                else if (keytoken.equals( "ship" ))
                    key = "SHIP" + lasttoken;
                else if (keytoken.equals( "settlement" ))
                    key = "SETTLEMENT" + lasttoken;
                else if (keytoken.equals( "city" ))
                    key = "CITY" + lasttoken;
                
                if (key == null)
                {
                    robotClient.sendText( game, "Unknown :consider-move type: " + keytoken );
                    return;
                }
                
                robotClient.sendRecordsText( gaName, key, true );
            }
            
            else if (dcmd.startsWith( ":last-target" ) || dcmd.startsWith( ":lt" ))
            {
                if ((brain != null) && (brain.getDRecorder().isOn()))
                {
                    SOCPossiblePiece lastTarget = brain.getLastTarget();
                    
                    if (lastTarget != null)
                    {
                        String key = null;
                        
                        switch (lastTarget.getType())
                        {
                        case SOCPossiblePiece.CARD:
                            key = "DEVCARD";
                            break;
                        
                        case SOCPossiblePiece.ROAD:
                            key = "ROAD" + lastTarget.getCoordinates();
                            break;
                        
                        case SOCPossiblePiece.SETTLEMENT:
                            key = "SETTLEMENT" + lastTarget.getCoordinates();
                            break;
                        
                        case SOCPossiblePiece.CITY:
                            key = "CITY" + lastTarget.getCoordinates();
                            break;
                        
                        case SOCPossiblePiece.SHIP:
                            key = "SHIP" + lastTarget.getCoordinates();
                            break;
                        }
                        
                        robotClient.sendRecordsText( gaName, key, false );
                    }
                }
                else
                {
                    robotClient.sendText( game, HINT_SEND_DEBUG_ON_FIRST );
                }
            }
            
            else if (dcmd.startsWith( ":consider-target " ) || dcmd.startsWith( ":ct " ))
            {
                String[] tokens = dcmd.split( " " );  // ":consider-target road 154"
                final int L = tokens.length;
                String keytoken = (L > 2) ? tokens[L - 2].trim() : "(missing)",
                        lasttoken = (L > 1) ? tokens[L - 1].trim() : "(missing)",
                        key = null;
                
                if (lasttoken.equals( "card" ))
                    key = "DEVCARD";
                else if (keytoken.equals( "road" ))
                    key = "ROAD" + lasttoken;
                else if (keytoken.equals( "ship" ))
                    key = "SHIP" + lasttoken;
                else if (keytoken.equals( "settlement" ))
                    key = "SETTLEMENT" + lasttoken;
                else if (keytoken.equals( "city" ))
                    key = "CITY" + lasttoken;
                
                 if (key == null)
                {
                    robotClient.sendText( game, "Unknown :consider-target type: " + keytoken );
                    return;
                }
                
                robotClient.sendRecordsText( gaName, key, false );
            }
            
            else if (dcmd.startsWith( ":print-vars" ) || dcmd.startsWith( ":pv" ))
            {
                // "prints" the results as series of SOCGameTextMsg to game
                robotClient.debugPrintBrainStatus( gaName, true, true );
            }
            
            else if (dcmd.startsWith( ":stats" ))
            {
                robotClient.sendStats( game );
            }
            
            else if (dcmd.startsWith( ":gc" ))
            {
                Runtime rt = Runtime.getRuntime();
                rt.gc();
                robotClient.sendText( game, "Free Memory:" + rt.freeMemory() );
            }
        }
    }
    
    /**
     * handle the "someone is sitting down" message
     * @param mes  the message
     */
    @Override
    protected SOCGame handleSITDOWN(SOCSitDown mes)
    {
        final String gaName = mes.getGameName();
        
        /**
         * tell the game that a player is sitting
         */
        final SOCGame ga = super.handleSITDOWN(mes);
        if (ga == null)
            return null;
        SOCRobotBrain brain = getClient().robotBrains.get( gaName );
        
        /**
         * let the robot brain find our player object if we sat down
         */
        final int pn = mes.getPlayerNumber();
        String nickname = getClient().getNickname();
        if (nickname.equals(mes.getNickname()))
        {
            
            if (brain.ourPlayerData != null)
            {
                if ((pn == brain.ourPlayerNumber) && nickname.equals(ga.getPlayer(pn).getName()))
                    return ga;  // already sitting in this game at this position, OK (can happen during loadgame)
                
                throw new IllegalStateException
                        ("bot " + nickname + " game " + gaName
                                 + ": got sitdown(pn=" + pn + "), but already sitting at pn=" + brain.ourPlayerNumber);
            }
            
            /**
             * retrieve the proper face for our strategy
             */
            int faceId;
            switch (brain.getRobotParameters().getStrategyType())
            {
            case SOCRobotDM.SMART_STRATEGY:
                faceId = -1;  // smarter robot face
                break;
            
            default:
                faceId = 0;   // default robot face
            }
            
            brain.setOurPlayerData();
            brain.start();
            
            /**
             * change our face to the robot face
             */
            getClient().put(new SOCChangeFace(ga.getName(), pn, faceId).toCmd());
        }
        else
        {
            /**
             * add tracker for player in previously vacant seat
             */
             if (brain != null)
                brain.addPlayerTracker(pn);
        }
        
        return ga;
    }

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    protected void handleDELETEGAME(SOCDeleteGame mes)
    {
        SOCRobotClient robotClient = getClient();
        SOCRobotBrain brain = robotClient.robotBrains.get(mes.getGameName());

        if (brain != null)
        {
            SOCGame game = robotClient.getGame( mes.getGameName() );

            if (game != null)
            {
                if (game.getGameState() == SOCGame.OVER)
                {
                    robotClient.gamesFinished++;

                    if (game.getPlayer(robotClient.getNickname()).getTotalVP() >= game.vp_winner)
                    {
                        robotClient.gamesWon++;
                        // TODO: should check actual winning player number (getCurrentPlayerNumber?)
                    }
                }

                brain.kill();
                robotClient.removeBrain( mes.getGameName() );     // also removes the brain from the brain queue
                robotClient.removeGame( mes.getGameName() );
            }
        }
    }


    /**
     * Handle the "game state" message; instead of immediately updating state,
     * calls {@link #handlePutBrainQ(SOCMessageForGame)}.
     * Can be overridden by third-party bots.
     * @param mes  the message
     */
    @Override
    protected void handleGAMESTATE(SOCGameState mes)
    {
        SOCGame game = getClient().getGame( mes.getGameName() );
        if (game != null)
        {
            handlePutBrainQ(mes);
        }
    }

    /**
     * handle the "dismiss robot" message
     * @param mes  the message
     */
    protected void handleROBOTDISMISS(SOCRobotDismiss mes)
    {
        String gameName = mes.getGameName();
        SOCGame ga = getClient().getGame( gameName );
        // probably should use a getter rather than accessing the field directly
        CappedQueue<SOCMessage> brainQ = getClient().brainQs.get(mes.getGameName());

        if ((ga != null) && (brainQ != null))
        {
            try
            {
                brainQ.put(mes);
            }
            catch (CutoffExceededException exc)
            {
                D.ebugPrintlnINFO("CutoffExceededException" + exc);
            }

            /**
             * if the brain isn't alive, then we need to leave
             * the game here, instead of having the brain leave it
             */
            SOCRobotBrain brain = getClient().robotBrains.get( gameName );

            if ((brain == null) || (! brain.isAlive()))
            {
                getClient().leaveGame( ga, "brain not alive in handleROBOTDISMISS", true, false);
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
     * Let server know we've done so, by sending LEAVEGAME via {@link SOCRobotClient#leaveGame(SOCGame, String, boolean, boolean)}.
     * Server will soon send a BOTJOINGAMEREQUEST if we should join the new game.
     *
     * @param mes  the message
     *
     * @see soc.server.SOCServer#resetBoardAndNotify(String, int)
     * @see SOCRobotClient#handleBOTJOINGAMEREQUEST(SOCBotJoinGameRequest)
     * @since 1.1.00
     */
    @Override
    protected void handleRESETBOARDAUTH(SOCResetBoardAuth mes)
    {
        D.ebugPrintlnINFO("**** handleRESETBOARDAUTH ****");
        
        String gameName = mes.getGameName();
        SOCGame game = getClient().getGame(gameName);
        if (game == null)
            return;  // Not one of our games
        
        SOCRobotBrain brain = getClient().robotBrains.get( gameName);
        if (brain != null)
            brain.kill();
        getClient().leaveGame(game, "resetboardauth", false, false);  // Same as in handleROBOTDISMISS
        game.destroyGame();
    }

    /** end of handler code **/




}
