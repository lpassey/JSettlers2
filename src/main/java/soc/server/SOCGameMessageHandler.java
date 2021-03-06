/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2016 Alessandro D'Ottavio
 * Some contents were formerly part of SOCServer.java and SOCGameHandler.java;
 * Portions of this file Copyright (C) 2003 Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2021 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012 Paul Bilnoski <paul@bilnoski.net>
 * Portions of this file Copyright (C) 2017-2018 Strategic Conversation (STAC Project) https://www.irit.fr/STAC/
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
package soc.server;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import soc.communication.Connection;
import soc.communication.SOCClientData;
import soc.debug.D;
import soc.game.GameState;
import soc.game.SOCBoardLarge;
import soc.game.SOCCity;
import soc.game.SOCDevCard;
import soc.game.SOCDevCardConstants;
import soc.game.SOCFortress;
import soc.game.SOCGame;
import soc.game.SOCGameOptionSet;
import soc.game.SOCInventoryItem;
import soc.game.SOCMoveRobberResult;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.game.SOCShip;
import soc.game.SOCSpecialItem;
import soc.game.SOCTradeOffer;
import soc.game.SOCVillage;
import soc.message.*;
import soc.message.SOCGameElements.GEType;
import soc.message.SOCPlayerElement.PEType;
import soc.util.SOCStringManager;

import static soc.game.GameState.*;

/**
 * Game message handler for {@link SOCGameHandler}: Dispatches all messages received from the
 * {@link soc.server.genericServer.InboundMessageQueue} related to specific games
 * (implementing {@link SOCMessageForGame}). All other messages are handled by
 * {@link SOCServerMessageHandler} instead.
 *
 *<H4>Message Flow:</H4>
 *<UL>
 * <LI> Inbound game messages arrive here from {@link SOCServerMessageDispatcher#dispatch(SOCMessage, Connection)}.
 * <LI> Each specific message class is identified in
 *      {@link #dispatch(SOCGame, SOCMessageForGame, Connection) dispatch(..)}
 *      which calls handler methods such as {@link #handleBANKTRADE(SOCGame, Connection, SOCBankTrade)}.
 *      See {@code dispatch(..)} method's javadoc for more details on per-message handling.
 * <LI> Most handler methods call into {@link SOCGameHandler} for the game's "business logic"
 *      abstracted from inbound message processing, and then {@link SOCServer} to send
 *      result messages to all players and observers in the game.
 *</UL>
 *
 * Before v2.0.00 this class was {@link SOCServer}{@code .processCommand(String, Connection)} and related
 * handler methods, all part of {@link SOCServer}. So, some may have {@code @since} javadoc labels with
 * versions older than 2.0.00. Refactoring for 2.0.00 in 2013 moved the handler methods from
 * {@link SOCServer} to {@link SOCGameHandler}, and in 2016 to this class.
 *
 * @see SOCServerMessageHandler
 * @author Alessandro D'Ottavio
 * @since 2.0.00
 */
public class SOCGameMessageHandler
    implements GameMessageHandler
{
    /** Server reference, for data and responses */
    private final SOCServer srv;

    /** Our SOCGameHandler */
    private final SOCGameHandler handler;

    public SOCGameMessageHandler( SOCServer srv, SOCGameHandler sgh )
    {
        this.srv = srv;
        handler = sgh;
    }

    /**
     * Dispatch any request or event coming from a client player for a specific game.
     * This method is called from {@link SOCServerMessageDispatcher#dispatch(SOCMessage, Connection)} when the message is
     * recognized as a game-related request, command, or event.
     *<P>
     * Some game messages (such as player sits down, or board reset voting) are handled the same for all game types.
     * These are handled by {@link SOCServerMessageHandler}; they should be ignored here and not appear in the
     * switch statement.
     *<P>
     * Caller of this method will catch any thrown Exceptions.
     *
     * @param game  Game in which client {@code connection} is sending {@code message}.
     *     Never null; from {@link SOCMessageForGame#getGameName()}.
     * @param message  Message from client {@code connection}. Never null.
     * @param connection  Connection to the Client sending {@code message}. Never null.
     *     {@link Connection#getData()} won't be {@code null}
     *     unless {@code message} implements {@link SOCMessageFromUnauthClient}.
     * @return true if processed, false if ignored or unknown message type
     */
    public boolean dispatch( SOCGame game, SOCMessageForGame message, Connection connection )
    {
        switch (message.getType())
        {

        /**
         * someone put a piece on the board
         */
        case SOCMessage.PUTPIECE:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handlePUTPIECE( game, connection, (SOCPutPiece) message );

            //ga = (SOCGame)gamesData.get(((SOCPutPiece)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCPutPiece)mes).getGame());
            break;

        /**
         * a player is moving the robber or pirate
         */
        case SOCMessage.MOVEROBBER:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleMOVEROBBER( game, connection, (SOCMoveRobber) message );

            //ga = (SOCGame)gamesData.get(((SOCMoveRobber)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCMoveRobber)mes).getGame());
            break;

        case SOCMessage.ROLLDICE:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleROLLDICE( game, connection, (SOCRollDice) message );

            //ga = (SOCGame)gamesData.get(((SOCRollDice)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCRollDice)mes).getGame());
            break;

        case SOCMessage.DISCARD:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleDISCARD( game, connection, (SOCDiscard) message );

            //ga = (SOCGame)gamesData.get(((SOCDiscard)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCDiscard)mes).getGame());
            break;

        case SOCMessage.ENDTURN:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleENDTURN( game, connection, (SOCEndTurn) message );

            //ga = (SOCGame)gamesData.get(((SOCEndTurn)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCEndTurn)mes).getGame());
            break;

        case SOCMessage.CHOOSEPLAYER:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleCHOOSEPLAYER( game, connection, (SOCChoosePlayer) message );

            //ga = (SOCGame)gamesData.get(((SOCChoosePlayer)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCChoosePlayer)mes).getGame());
            break;

        case SOCMessage.MAKEOFFER:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleMAKEOFFER( game, connection, (SOCMakeOffer) message );

            //ga = (SOCGame)gamesData.get(((SOCMakeOffer)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCMakeOffer)mes).getGame());
            break;

        case SOCMessage.CLEAROFFER:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleCLEAROFFER( game, connection, (SOCClearOffer) message );

            //ga = (SOCGame)gamesData.get(((SOCClearOffer)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCClearOffer)mes).getGame());
            break;

        case SOCMessage.REJECTOFFER:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleREJECTOFFER( game, connection, (SOCRejectOffer) message );

            //ga = (SOCGame)gamesData.get(((SOCRejectOffer)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCRejectOffer)mes).getGame());
            break;

        case SOCMessage.ACCEPTOFFER:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleACCEPTOFFER( game, connection, (SOCAcceptOffer) message );

            //ga = (SOCGame)gamesData.get(((SOCAcceptOffer)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCAcceptOffer)mes).getGame());
            break;

        case SOCMessage.BANKTRADE:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleBANKTRADE( game, connection, (SOCBankTrade) message );

            //ga = (SOCGame)gamesData.get(((SOCBankTrade)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCBankTrade)mes).getGame());
            break;

        case SOCMessage.BUILDREQUEST:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleBUILDREQUEST( game, connection, (SOCBuildRequest) message );

            //ga = (SOCGame)gamesData.get(((SOCBuildRequest)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCBuildRequest)mes).getGame());
            break;

        case SOCMessage.CANCELBUILDREQUEST:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleCANCELBUILDREQUEST( game, connection, (SOCCancelBuildRequest) message );

            //ga = (SOCGame)gamesData.get(((SOCCancelBuildRequest)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCCancelBuildRequest)mes).getGame());
            break;

        case SOCMessage.BUYDEVCARDREQUEST:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handleBUYDEVCARDREQUEST( game, connection, (SOCBuyDevCardRequest) message );

            //ga = (SOCGame)gamesData.get(((SOCBuyDevCardRequest)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCBuyDevCardRequest)mes).getGame());
            break;

        case SOCMessage.PLAYDEVCARDREQUEST:

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handlePLAYDEVCARDREQUEST( game, connection, (SOCPlayDevCardRequest) message );

            //ga = (SOCGame)gamesData.get(((SOCPlayDevCardRequest)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCPlayDevCardRequest)mes).getGame());
            break;

        case SOCMessage.PICKRESOURCES:  // Discovery / Year of Plenty / Gold Hex resource picks

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handlePICKRESOURCES( game, connection, (SOCPickResources) message );

            //ga = (SOCGame)gamesData.get(((SOCPickResources)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCPickResources)mes).getGame());
            break;

        case SOCMessage.PICKRESOURCETYPE:  // Resource Type / Monopoly pick

            //createNewGameEventRecord();
            //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
            handlePICKRESOURCETYPE( game, connection, (SOCPickResourceType) message );

            //ga = (SOCGame)gamesData.get(((SOCPickResourceType)mes).getGame());
            //currentGameEventRecord.setSnapshot(ga);
            //saveCurrentGameEventRecord(((SOCPickResourceType)mes).getGame());
            break;

        /**
         * debug piece Free Placement (as of 20110104 (v 1.1.12))
         */
        case SOCMessage.DEBUGFREEPLACE:
            handleDEBUGFREEPLACE( game, connection, (SOCDebugFreePlace) message );
            break;

        /**
         * Generic simple request from a player.
         * Added 2013-02-17 for v1.1.18.
         */
        case SOCMessage.SIMPLEREQUEST:
            handleSIMPLEREQUEST( game, connection, (SOCSimpleRequest) message );
            break;

        /**
         * Special inventory item action (play request) from a player.
         * Added 2013-11-28 for v2.0.00.
         */
        case SOCMessage.INVENTORYITEMACTION:
            handleINVENTORYITEMACTION( game, connection, (SOCInventoryItemAction) message );
            break;

        /**
         * Asking to move a previous piece (a ship) somewhere else on the board.
         * Added 2011-12-04 for v2.0.00.
         */
        case SOCMessage.MOVEPIECE:
            handleMOVEPIECE( game, connection, (SOCMovePiece) message );
            break;

        /**
         * Special Item requests.
         * Added 2014-05-17 for v2.0.00.
         */
        case SOCMessage.SETSPECIALITEM:
            handleSETSPECIALITEM( game, connection, (SOCSetSpecialItem) message );
            break;

        /**
         * Ignore all other message types, unknown message types.
         */
        default:
            return false;

        }  // switch (mes.getType)

        return true;  // Message was handled in a non-default case above
    }


    /// Roll dice and pick resources ///


    /**
     * handle "roll dice" message.
     *
     * @param game the game instance that this message is for
     * @param c  the connection that sent the message
     * @param rollDice  the SOCRollDice message
     * @since 1.0.0
     */
    private void handleROLLDICE( SOCGame game, Connection c,
        @SuppressWarnings("unused") SOCRollDice rollDiceMessage )
    {
        final String gn = game.getName();

        game.takeMonitor();

        try
        {
            final String plName = c.getData();
            final SOCPlayer pl = game.getPlayer( plName );
            if ((pl != null) && game.canRollDice( pl.getPlayerNumber() ))
            {
                /**
                 * Roll dice, distribute resources in game. Typically sets game state to
                 * GameState.SENDING_DICE_RESULT_RESOURCES;
                 */
                SOCGame.RollResult roll = game.rollDice();

                /**
                 * Send roll results and then text to client.
                 * Note that only the total is sent, not the 2 individual dice.
                 * (Only the _SC_PIRI scenario cares about them indivdually, and
                 * in that case it prints the result when needed.)
                 *
                 * If a 7 is rolled, sendGameState will also say who must discard
                 * (in a GAMETEXTMSG).
                 * If a gold hex is rolled, sendGameState will also say who
                 * must pick resources to gain (in a GAMETEXTMSG).
                 */
                srv.messageToGame( gn, true, new SOCDiceResult( gn, game.getCurrentDice() ) );
//                if (game.clientVersionLowest < SOCGameTextMsg.VERSION_FOR_DICE_RESULT_INSTEAD)
//                {
//                    // backwards-compat: this text message is redundant to v2.0.00 and newer clients
//                    // because they print the roll results from SOCDiceResult.  Use SOCGameTextMsg
//                    // because pre-2.0.00 clients don't understand SOCGameServerText messages.
//                    srv.messageToGameForVersions( game, 0, SOCGameTextMsg.VERSION_FOR_DICE_RESULT_INSTEAD - 1,
//                        new SOCGameTextMsg
//                            ( gn, SOCGameTextMsg.SERVERNAME,
//                             plName + " rolled a " + roll.diceA + " and a " + roll.diceB + "."), // I18N OK: v1.x always english
//                        true );
//                }
                handler.sendGameState( game );  // For 7, give visual feedback before sending discard request

                /* if true but noPlayersGained, will change announcement wording from "No player gets anything". */
                boolean someoneWonFreeRsrc = false;

                if (game.isGameOptionSet( SOCGameOptionSet.K_SC_PIRI ))
                {
                    // pirate moves on every roll
                    srv.messageToGame( gn, true, new SOCMoveRobber
                        ( gn, game.getCurrentPlayerNumber(), -(((SOCBoardLarge) game.getBoard()).getPirateHex()) ) );

                    if (roll.sc_piri_fleetAttackVictim != null)
                    {
                        final SOCResourceSet loot = roll.sc_piri_fleetAttackRsrcs;
                        final int lootTotal = (loot != null) ? loot.getTotal() : 0;
                        if (lootTotal != 0)
                        {
                            final boolean won = (loot.contains( SOCResourceConstants.GOLD_LOCAL ));
                            SOCPlayer vic = roll.sc_piri_fleetAttackVictim;
                            final String vicName = vic.getName();
                            final Connection vCon = srv.getConnection( vicName );
                            final int vVersion = (vCon != null) ? vCon.getRemoteVersion() : 0;
                            final int vpn = vic.getPlayerNumber();
                            final int strength = (roll.diceA < roll.diceB) ? roll.diceA : roll.diceB;

                            if (won)
                            {
                                someoneWonFreeRsrc = true;

                                // Special form of SOCReportRobbery to notify newer clients of win
                                SOCReportRobbery wonMsg = new SOCReportRobbery
                                    ( gn, -1, vpn, SOCResourceConstants.UNKNOWN, true, 0, 0, strength );
                                if (game.clientVersionLowest >= SOCReportRobbery.MIN_VERSION)
                                {
                                    srv.messageToGame( gn, true, wonMsg );
                                }
                                else
                                {
                                    srv.messageToGameForVersions
                                        ( game, SOCReportRobbery.MIN_VERSION, Integer.MAX_VALUE, wonMsg, true );
                                    srv.messageToGameForVersionsKeyed
                                        ( game, -1, SOCReportRobbery.MIN_VERSION - 1, true,
                                            false, "action.rolled.sc_piri.player.won.pick.free", vicName, strength );
                                    // "{0} won against the pirate fleet (strength {1}) and will pick a free resource."
                                    srv.recordGameEvent( gn, wonMsg );
                                }
                            }
                            else
                            {
                                // Use SOCReportRobbery if clients new enough, otherwise
                                // use text and same resource-loss messages sent in handleDISCARD.
                                // If fully observable (game opt PLAY_FO), announce details to everyone.

                                final SOCReportRobbery reportDetails = new SOCReportRobbery
                                    ( gn, -1, vpn, loot, strength ),
                                    reportUnknown = new SOCReportRobbery
                                        ( gn, -1, vpn, SOCResourceConstants.UNKNOWN, true, lootTotal, 0, strength );

                                if (game.clientVersionLowest >= SOCReportRobbery.MIN_VERSION)
                                {
                                    if (game.isGameOptionSet( SOCGameOptionSet.K_PLAY_FO ))
                                    {
                                        srv.messageToGame( gn, true, reportDetails );
                                    }
                                    else
                                    {
                                        srv.messageToPlayer( vCon, gn, vpn, reportDetails );
                                        srv.messageToGameExcept( gn, vCon, vpn, reportUnknown, true );
                                    }
                                }
                                else
                                {
                                    if (game.isGameOptionSet( SOCGameOptionSet.K_PLAY_FO ))
                                    {
                                        srv.recordGameEvent( gn, reportDetails );
                                        srv.messageToGameForVersions
                                            ( game, SOCReportRobbery.MIN_VERSION, Integer.MAX_VALUE, reportDetails, true );
                                        handler.reportRsrcGainLossForVersions
                                            ( game, loot, true, true, vpn, -1, null, SOCReportRobbery.MIN_VERSION - 1 );
                                        if (vVersion < SOCReportRobbery.MIN_VERSION)
                                            srv.messageToPlayerKeyedSpecial
                                                ( vCon, game, vpn, "action.rolled.sc_piri.you.lost.rsrcs.to.fleet", loot, strength );
                                        // "You lost {0,rsrcs} to the pirate fleet (strength {1,number})."
                                    }
                                    else
                                    {
                                        // tell the victim which resources they lost
                                        if (vVersion >= SOCReportRobbery.MIN_VERSION)
                                        {
                                            srv.messageToPlayer( vCon, gn, vpn, reportDetails );
                                        }
                                        else
                                        {
                                            srv.recordGameEventTo( gn, vpn, reportDetails );
                                            handler.reportRsrcGainLossForVersions
                                                ( game, loot, true, true, vpn, -1, vCon, SOCReportRobbery.MIN_VERSION - 1 );
                                            srv.messageToPlayerKeyedSpecial
                                                ( vCon, game, vpn, "action.rolled.sc_piri.you.lost.rsrcs.to.fleet", loot, strength );
                                            // "You lost {0,rsrcs} to the pirate fleet (strength {1,number})."
                                        }

                                        // tell everyone else they lost unknown resources
                                        srv.recordGameEventNotTo( gn, vpn, reportUnknown );
                                        srv.messageToGameForVersionsExcept
                                            ( game, SOCReportRobbery.MIN_VERSION, Integer.MAX_VALUE, vCon, reportUnknown, true );
                                        srv.messageToGameForVersionsExcept
                                            ( game, -1, SOCReportRobbery.MIN_VERSION - 1, vCon,
                                                new SOCPlayerElement
                                                    ( gn, vpn, SOCPlayerElement.LOSE, PEType.UNKNOWN_RESOURCE, lootTotal ),
                                                true );
                                    }

                                    srv.messageToGameForVersionsKeyedExcept
                                        ( game, -1, SOCReportRobbery.MIN_VERSION - 1, true,
                                            Arrays.asList( vCon ), false,
                                            "action.rolled.sc_piri.player.lost.rsrcs.to.fleet", vicName, lootTotal, strength );
                                    // "Joe lost 1 resource to pirate fleet attack (strength 3)." or
                                    // "Joe lost 3 resources to pirate fleet attack (strength 3)."
                                }
                            }
                        }
                    }
                }

                /**
                 * if the roll is not 7, tell players what they got
                 * (if 7, sendGameState already told them what they lost).
                 */
                if (game.getCurrentDice() != 7)
                {
                    boolean noPlayersGained = true;  // see also someoneWonFreeRsrc

                    /**
                     * Clients v2.0.00 and newer get an i18n-neutral SOCDiceResultResources message.
                     * Older clients get a string such as "Joe gets 3 sheep. Mike gets 1 clay."
                     */
                    String rollRsrcTxtOldCli = null;
                    SOCDiceResultResources rollRsrcMsgNewCli = null;

                    if (game.clientVersionHighest >= SOCDiceResultResources.VERSION_FOR_DICERESULTRESOURCES)
                    {
                        rollRsrcMsgNewCli = SOCDiceResultResources.buildForGame( game );
                        noPlayersGained = (rollRsrcMsgNewCli == null);
                    }

//                    if (game.clientVersionLowest < SOCDiceResultResources.VERSION_FOR_DICERESULTRESOURCES)
//                    {
//                        // Build a string to announce to v1.x.xx clients
//                        StringBuilder gainsText = new StringBuilder();
//
//                        noPlayersGained = true;  // for string spacing; might be false due to loop for new clients in game
//                        for (int pn = 0; pn < game.maxPlayers; ++pn)
//                        {
//                            if (!game.isSeatVacant( pn ))
//                            {
//                                SOCPlayer pp = game.getPlayer( pn );
//                                SOCResourceSet rsrcs = pp.getRolledResources();
//
//                                if (rsrcs.getKnownTotal() != 0)
//                                {
//                                    if (noPlayersGained)
//                                        noPlayersGained = false;
//                                    else
//                                        gainsText.append( " " );
//
//                                    gainsText.append
//                                        ( c.getLocalizedSpecial( game, "_nolocaliz.roll.gets.resources", pp.getName(), rsrcs ) );
//                                    // "{0} gets {1,rsrcs}."
//                                    // get it from any connection's StringManager, because that string is never localized
//
//                                    // Announce SOCPlayerElement.GAIN messages
//                                    handler.reportRsrcGainLoss( game, rsrcs, false, false, pn, -1, null );
//                                }
//                            }
//                        }
//
//                        if (!noPlayersGained)
//                            rollRsrcTxtOldCli = gainsText.toString();
//
//                    }

                    if (noPlayersGained)
                    {
                        // send text message
                        String key;
                        if (roll.cloth == null)
                            key = (someoneWonFreeRsrc)
                                ? "action.rolled.no_other_player_gets.anything"  // "No other player gets anything."
                                : "action.rolled.no_player_gets.anything";       // "No player gets anything."
                        else
                            key = (someoneWonFreeRsrc)
                                ? "action.rolled.no_other_player_gets.resources"  // "No other player gets resources."
                                : "action.rolled.no_player_gets.resources";       // "No player gets resources."
                        // debug_printPieceDiceNumbers(ga, message);
                        srv.messageToGameKeyed( game, true, true, key );
                    }
                    else
                    {
                        if (rollRsrcTxtOldCli == null)
                            srv.messageToGame( gn, true, rollRsrcMsgNewCli );
                        else if (rollRsrcMsgNewCli == null)
                            srv.messageToGame( gn, true, rollRsrcTxtOldCli );
                        else
                        {
                            // neither is null: we have old and new clients
                            srv.messageToGameForVersions
                                ( game, 0, (SOCDiceResultResources.VERSION_FOR_DICERESULTRESOURCES - 1),
                                    new SOCGameTextMsg( gn, SOCGameTextMsg.SERVERNAME, rollRsrcTxtOldCli ), true );
                            srv.messageToGameForVersions
                                ( game, SOCDiceResultResources.VERSION_FOR_DICERESULTRESOURCES, Integer.MAX_VALUE,
                                    rollRsrcMsgNewCli, true );

                            srv.recordGameEvent( gn, rollRsrcMsgNewCli );
                        }

                        //
                        //  Send gaining players all their resource info for accuracy
                        //  and announce their new resource totals to game
                        //
                        for (int pn = 0; pn < game.maxPlayers; ++pn)
                        {
                            final SOCPlayer pp = game.getPlayer( pn );
                            if (pp.getRolledResources().getKnownTotal() == 0)
                                continue;  // skip if player didn't gain; before v2.0.00 each player in game got these
                            final Connection playerCon = srv.getConnection( pp.getName() );
                            if (playerCon == null)
                                continue;

                            // send CLAY, ORE, SHEEP, WHEAT, WOOD even if player's amount is 0
                            final SOCResourceSet resources = pp.getResources();
                            final int[] counts = resources.getAmounts( false );
                            if (playerCon.getRemoteVersion() >= SOCPlayerElements.MIN_VERSION)
                            {
                                srv.messageToPlayer( playerCon, gn, pn, new SOCPlayerElements
                                    ( gn, pn, SOCPlayerElement.SET, SOCGameHandler.ELEM_RESOURCES, counts ) );
                            }
                            else
                            {
                                for (int i = 0; i < counts.length; ++i)
                                    srv.messageToPlayer
                                        ( playerCon, null, SOCServer.PN_NON_EVENT,
                                            new SOCPlayerElement
                                                ( gn, pn, SOCPlayerElement.SET, SOCGameHandler.ELEM_RESOURCES[i], counts[i] ) );

                                if (srv.recordGameEventsIsActive())
                                    srv.recordGameEvent( gn, new SOCPlayerElements
                                        ( gn, pn, SOCPlayerElement.SET, SOCGameHandler.ELEM_RESOURCES, counts ) );
                            }

                            if (game.clientVersionLowest < SOCDiceResultResources.VERSION_FOR_DICERESULTRESOURCES)
                                srv.messageToGame( gn, false, new SOCResourceCount( gn, pn, resources.getTotal() ) );
                            // else, already-sent SOCDiceResultResources included players' new resource totals

                            if ((rollRsrcMsgNewCli == null) && srv.recordGameEventsIsActive())
                                srv.recordGameEvent( gn, new SOCResourceCount( gn, pn, resources.getTotal() ) );

                            // we'll send gold picks text, PLAYERELEMENT, and SIMPLEREQUEST(PROMPT_PICK_RESOURCES)
                            // after the per-player loop
                        }
                    }

                    if (roll.cloth != null)
                    {
                        // Send village cloth trade distribution

                        if (roll.clothVillages != null)
                            for (final SOCVillage vi : roll.clothVillages)
                                srv.messageToGame( gn, true, new SOCPieceValue
                                    ( gn, SOCPlayingPiece.VILLAGE, vi.getCoordinates(), vi.getCloth(), 0 ) );

                        if (roll.cloth[0] > 0)
                            // some taken from board general supply
                            srv.messageToGame( gn, true, new SOCPlayerElement
                                ( gn, -1, SOCPlayerElement.SET, PEType.SCENARIO_CLOTH_COUNT,
                                    ((SOCBoardLarge) game.getBoard()).getCloth() ) );

                        String clplName = null;   // name of first player to receive cloth
                        int clplAmount = 0;
                        ArrayList<String> clpls = null;  // names of all players receiving cloth, if more than one
                        for (int i = 1; i < roll.cloth.length; ++i)
                        {
                            if (roll.cloth[i] == 0)
                                continue;  // this player didn't receive cloth

                            final int pn = i - 1;
                            final SOCPlayer clpl = game.getPlayer( pn );
                            srv.messageToGame( gn, true, new SOCPlayerElement
                                ( gn, pn, SOCPlayerElement.SET, PEType.SCENARIO_CLOTH_COUNT, clpl.getCloth() ) );

                            if (clplName == null)
                            {
                                // first pl to receive cloth
                                clplName = clpl.getName();
                                clplAmount = roll.cloth[i];
                            }
                            else
                            {
                                // second or further player
                                if (clpls == null)
                                {
                                    clpls = new ArrayList<>();
                                    clpls.add( clplName );
                                }
                                clpls.add( clpl.getName() );
                            }
                        }

                        if (clpls == null)
                            srv.messageToGameKeyed
                                ( game, true, true, "action.rolled.sc_clvi.received.cloth.1", clplName, clplAmount );
                            // "{0} received {1} cloth from the villages."
                        else
                            srv.messageToGameKeyedSpecial
                                ( game, true, true, "action.rolled.sc_clvi.received.cloth.n", clpls );
                        // "{0,list} each received cloth from the villages."
                    }

                    if (game.getGameState() == WAITING_FOR_PICK_GOLD_RESOURCE)
                        // gold picks text, PLAYERELEMENT, and SIMPLEREQUEST(PROMPT_PICK_RESOURCES)s
                        handler.sendGameState_sendGoldPickAnnounceText( game, gn, null, roll );

                    /*
                       if (D.ebugOn) {
                       for (int i=0; i < GameState.MAXPLAYERS; i++) {
                       SOCResourceSet rsrcs = ga.getPlayer(i).getResources();
                       String resourceMessage = "PLAYER "+i+" RESOURCES: ";
                       resourceMessage += rsrcs.getAmount(SOCResourceConstants.CLAY)+" ";
                       resourceMessage += rsrcs.getAmount(SOCResourceConstants.ORE)+" ";
                       resourceMessage += rsrcs.getAmount(SOCResourceConstants.SHEEP)+" ";
                       resourceMessage += rsrcs.getAmount(SOCResourceConstants.WHEAT)+" ";
                       resourceMessage += rsrcs.getAmount(SOCResourceConstants.WOOD)+" ";
                       resourceMessage += rsrcs.getAmount(SOCResourceConstants.UNKNOWN)+" ";
                       messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, resourceMessage));
                       }
                       }
                     */
                    else
                    {
                        game.setGameState( PLAY1 );     // only allow play after all updates.
                        handler.sendGameState( game );
                    }
                }
                else
                {
                    /**
                     * player rolled 7
                     * If anyone needs to discard, prompt them.
                     */
                    if (game.getGameState() == WAITING_FOR_DISCARDS)
                    {
                        handler.sendGameState_sendDiscardRequests( game, gn );
                    }
                    else if (game.getGameState() == WAITING_FOR_PICK_GOLD_RESOURCE)
                    {
                        // Used in _SC_PIRI, when 7 is rolled and a player wins against the pirate fleet
                        for (int pn = 0; pn < game.maxPlayers; ++pn)
                        {
                            final SOCPlayer pp = game.getPlayer( pn );
                            final int numPick = pp.getNeedToPickGoldHexResources();
                            if ((!game.isSeatVacant( pn )) && (numPick > 0))
                            {
                                Connection con = srv.getConnection( pp.getName() );
                                if (con != null)
                                {
                                    srv.messageToGame( gn, true, new SOCPlayerElement
                                        ( gn, pn, SOCPlayerElement.SET,
                                            PEType.NUM_PICK_GOLD_HEX_RESOURCES, numPick ) );
                                    srv.messageToPlayer
                                        ( con, gn, pn, new SOCSimpleRequest
                                            ( gn, pn, SOCSimpleRequest.PROMPT_PICK_RESOURCES, numPick, 0 ) );
                                }
                            }
                        }
                    }
                }

                if (game.clientRequestsDiceResultsFullySent)
                    srv.messageToGame( gn, true, new SOCSimpleAction
                        ( gn, -1, SOCSimpleAction.DICE_RESULTS_FULLY_SENT ) );
            }
            else if (null != pl)
            {
                srv.messageToPlayer( c, gn, pl.getPlayerNumber(), "You can't roll right now." );
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught at handleROLLDICE" + e );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "discard" message.
     *
     * @param game the game instance that this message is for
     * @param c  the connection that sent the message
     * @param discardMsg  the message
     * @since 1.0.0
     */
    private void handleDISCARD( SOCGame game, Connection c, final SOCDiscard discardMsg )
    {
        final String gameName = game.getName();
        final SOCPlayer player = game.getPlayer( c.getData() );
        final int pn;
        if (player != null)
            pn = player.getPlayerNumber();
        else
            pn = -1;  // c's client no longer in the game

        game.takeMonitor();
        try
        {
            if (player == null)
            {
                // The catch block will print this out semi-nicely
                throw new IllegalArgumentException( "player not found in game" );
            }

            final SOCResourceSet res = discardMsg.getResources();
            if (game.canDiscard( pn, res ))
            {
                game.discard( pn, res );  // discard, maybe change gameState

                // Same resource-loss messages are sent in handleROLLDICE after a pirate fleet attack (_SC_PIRI).

                final int numRes = res.getTotal();
                if (game.isGameOptionSet( SOCGameOptionSet.K_PLAY_FO ))
                {
                    // fully observable: announce to everyone
                    handler.reportRsrcGainLoss( game, res, true, false, pn, -1, null );
                }
                else
                {
                    // tell the player client that the player discarded the resources
                    handler.reportRsrcGainLoss( game, res, true, false, pn, -1, c );

                    // tell everyone else that the player discarded unknown resources
                    srv.messageToGameExcept
                        ( gameName, c, pn, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.LOSE, PEType.UNKNOWN_RESOURCE, numRes, true ),
                            true );
                }
                srv.messageToGameKeyed( game, true, true, "action.discarded", player.getName(), numRes );
                // "{0} discarded {1} resources."

                /**
                 * send the new state, or end turn if was marked earlier as forced
                 */
                final GameState gstate = game.getGameState();
                if ((gstate != PLAY1) || !game.isForcingEndTurn())
                {
                    if (gstate == WAITING_FOR_DISCARDS)
                        handler.sendGameState( game, true, false );  // send only text prompt, not redundant GAMESTATE
                    else
                        handler.sendGameState( game );
                    // if state is WAITING_FOR_ROB_CHOOSE_PLAYER (_SC_PIRI), also sends CHOOSEPLAYERREQUEST
                }
                else
                {
                    handler.endGameTurn( game, player, true );  // already did ga.takeMonitor()
                }
            }
            else if (game.getGameState() == WAITING_FOR_DISCARDS)
            {
                // shouldn't occur: client was told how many discards are needed, if any
                srv.messageToPlayer( c, gameName, pn, "You can't discard that many cards." );  // I18N OK: not part of normal message flow
                final int n = player.getCountToDiscard();
                if (n > 0)
                    srv.messageToPlayer( c, gameName, pn, new SOCDiscardRequest( gameName, n ) );
            }
        }
        catch( Throwable e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }


    /// Robber/pirate robbery ///


    /**
     * handle "move robber" message (move the robber or the pirate).
     *
     * @param game the game instance that this message is for
     * @param c  the connection that sent the message
     * @param moveRobberMsg  the message
     * @since 1.0.0
     */
    private void handleMOVEROBBER( SOCGame game, Connection c, SOCMoveRobber moveRobberMsg )
    {
        game.takeMonitor();

        try
        {
            SOCPlayer player = game.getPlayer( c.getData() );

            /**
             * make sure the player can do it
             */
            final String gaName = game.getName();
            final boolean isPirate = game.getRobberyPirateFlag();
            final int pn = player.getPlayerNumber();
            int coord = moveRobberMsg.getCoordinates();  // negative for pirate
            final boolean canDo =
                (isPirate == (coord < 0))
                    && (isPirate ? game.canMovePirate( pn, -coord )
                    : game.canMoveRobber( pn, coord ));
            if (canDo)
            {
                SOCMoveRobberResult result;

                SOCMoveRobber moveMsg;
                if (isPirate)
                {
                    result = game.movePirate( pn, -coord );
                    moveMsg = new SOCMoveRobber( gaName, pn, coord );
                }
                else
                {
                    result = game.moveRobber( pn, coord );    // changes gameState!
                    moveMsg = new SOCMoveRobber( gaName, pn, coord );
                }
                srv.messageToGame( gaName, true, moveMsg );

                final List<SOCPlayer> victims = result.getVictims();

                /** only one possible victim */
                if ((victims.size() == 1) && (game.getGameState() != WAITING_FOR_ROB_CLOTH_OR_RESOURCE))
                {
                    /**
                     * report what was stolen
                     */
                    SOCPlayer victim = victims.get( 0 );
                    handler.reportRobbery( game, player, victim, result.getLoot() );
                }
                else
                {
                    final String msgKey;
                    // These messages use ChoiceFormat to choose "robber" or "pirate":
                    //    robberpirate.moved={0} moved {1,choice, 1#the robber|2#the pirate}.

                    /** no victim */
                    if (victims.size() == 0)
                    {
                        /**
                         * just say it was moved; nothing is stolen
                         */
                        msgKey = "robberpirate.moved";  // "{0} moved the robber" or "{0} moved the pirate"
                        game.setGameState( PLAY1 );     // No more resource updates forthcoming.
                    }
                    else if (game.getGameState() == WAITING_FOR_ROB_CLOTH_OR_RESOURCE)
                    {
                        /**
                         * only one possible victim, they have both clay and resources
                         */
                        msgKey = "robberpirate.moved.choose.cloth.rsrcs";
                        // "{0} moved the robber/pirate. Must choose to steal cloth or steal resources."
                    }
                    else
                    {
                        /**
                         * else, the player needs to choose a victim
                         */
                        msgKey = "robberpirate.moved.choose.victim";
                        // "{0} moved the robber/pirate. Must choose a victim."
                    }

                    srv.messageToGameKeyed( game, true, true, msgKey, player.getName(), ((isPirate) ? 2 : 1) );
                }

                handler.sendGameState( game );
                // For WAITING_FOR_ROB_CHOOSE_PLAYER, sendGameState also sends messages
                // with victim info to prompt the client to choose.
                // For WAITING_FOR_ROB_CLOTH_OR_RESOURCE, no need to recalculate
                // victims there, just send the prompt from here:
                if (game.getGameState() == WAITING_FOR_ROB_CLOTH_OR_RESOURCE)
                {
                    final int vpn = victims.get( 0 ).getPlayerNumber();
                    srv.messageToPlayer( c, gaName, pn, new SOCChoosePlayer( gaName, vpn ) );
                }
            }
            else
            {
                srv.messageToPlayerKeyed
                    ( c, gaName, pn, ((coord < 0) ? "robber.cantmove.pirate" : "robber.cantmove") );
                // "You can't move the pirate" / "You can't move the robber"
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "choose player" message during robbery.
     *
     * @param game the game instance that this message is for
     * @param c  the connection that sent the message
     * @param choosePlayerMsg  the message
     * @since 1.0.0
     */
    @SuppressWarnings("fallthrough")
    private void handleCHOOSEPLAYER( SOCGame game, Connection c, final SOCChoosePlayer choosePlayerMsg )
    {
        game.takeMonitor();

        try
        {
            final String gaName = game.getName();

            if (handler.checkTurn( c, game ))
            {
                final int choice = choosePlayerMsg.getChoice();
                switch (game.getGameState())
                {
                case WAITING_FOR_ROBBER_OR_PIRATE:  // changes game state!
                    game.chooseMovePirate( choice == SOCChoosePlayer.CHOICE_MOVE_PIRATE );
                    handler.sendGameState( game );
                    break;

                case WAITING_FOR_ROB_CHOOSE_PLAYER:
                    if ((choice == SOCChoosePlayer.CHOICE_NO_PLAYER) && game.canChoosePlayer( -1 ))
                    {
                        game.choosePlayerForRobbery( -1 );  // state becomes PLAY1
                        srv.messageToGameKeyed
                            ( game, true, true, "robber.declined", c.getData() );  // "{0} declined to steal."
                        handler.sendGameState( game );
                    }
                    else if (game.canChoosePlayer( choice ))
                    {
                        final int rsrc = game.choosePlayerForRobbery( choice );
                        final boolean waitingClothOrRsrc =
                            (game.getGameState() == WAITING_FOR_ROB_CLOTH_OR_RESOURCE);
                        if (!waitingClothOrRsrc)
                        {
                            handler.reportRobbery
                                ( game, game.getPlayer( c.getData() ), game.getPlayer( choice ), rsrc );
                        }
                        else
                        {
                            srv.messageToGameKeyed( game, true, true, "robber.moved.choose.cloth.rsrcs",
                                c.getData(), game.getPlayer( choice ).getName() );
                            // "{0} moved the pirate, must choose to steal cloth or steal resources from {1}."
                        }
                        handler.sendGameState( game );
                        if (waitingClothOrRsrc)
                            srv.messageToPlayer
                                ( c, gaName, game.getCurrentPlayerNumber(), new SOCChoosePlayer( gaName, choice ) );
                    }
                    else
                    {
                        srv.messageToPlayerKeyed
                            ( c, gaName, game.getCurrentPlayerNumber(), "robber.cantsteal" );
                        // "You can't steal from that player."
                    }
                    break;

                case WAITING_FOR_ROB_CLOTH_OR_RESOURCE:
                {
                    final boolean stealCloth;
                    final int pn;
                    if (choice < 0)
                    {
                        stealCloth = true;
                        pn = (-choice) - 1;
                    }
                    else
                    {
                        stealCloth = false;
                        pn = choice;
                    }
                    if (game.canChoosePlayer( pn ) && game.canChooseRobClothOrResource( pn ))
                    {
                        final int rsrc = game.stealFromPlayer( pn, stealCloth );
                        handler.reportRobbery
                            ( game, game.getPlayer( c.getData() ), game.getPlayer( pn ), rsrc );
                        handler.sendGameState( game );
                        break;
                    }
                    // else, fall through and send "can't steal" message
                }

                default:
                    srv.messageToPlayerKeyed
                        ( c, gaName, game.getCurrentPlayerNumber(), "robber.cantsteal" );
                    // "You can't steal from that player."
                }
            }
            else
            {
                srv.messageToPlayerKeyed
                    ( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED, "base.reply.not.your.turn" );  // "It's not your turn."
            }
        }
        catch( Throwable e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }


    /// Flow of Game ///


    /**
     * handle "end turn" message.
     * This normally ends a player's normal turn (phase {@link SOCGame#PLAY1}).
     * On the 6-player board, it ends their placements during the
     * {@link SOCGame#SPECIAL_BUILDING Special Building Phase}.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param endTurnMessage  the message
     * @since 1.0.0
     */
    private void handleENDTURN( SOCGame game, Connection c, @SuppressWarnings("unused") SOCEndTurn endTurnMessage )
    {
        final String gname = game.getName();

        if (game.isDebugFreePlacement())
        {
            // turn that off before ending current turn
            handler.processDebugCommand_freePlace( c, game, "0" );
        }

        game.takeMonitor();

        final GameState gaState = game.getGameState();

        try
        {
            final String plName = c.getData();
            if (gaState == GAME_OVER)
            {
                // Should not happen; is here just in case.
                SOCPlayer pl = game.getPlayer( plName );
                if (pl != null)
                {
                    String msg = game.gameOverMessageToPlayer( pl );
                    // msg = "The game is over; you are the winner!";
                    // msg = "The game is over; <someone> won.";
                    // msg = "The game is over; no one won.";
                    srv.messageToPlayer( c, gname, SOCServer.PN_REPLY_TO_UNDETERMINED, msg );
                }
            }
            else if (handler.checkTurn( c, game ))
            {
                SOCPlayer pl = game.getPlayer( plName );
                if ((pl != null) && game.canEndTurn( pl.getPlayerNumber() ))
                {
                    if (gaState == PLACING_FREE_ROAD1)
                        srv.messageToGameKeyed( game, true, true, "action.card.roadbuilding.cancel", pl.getName() );
                        // "{0} cancelled the Road Building card."
                    else if (gaState == PLACING_FREE_ROAD2)
                        srv.messageToGameKeyed( game, true, true, "action.card.roadbuilding.skip.r", pl.getName() );
                    // "{0} skipped placing the second road."

                    handler.endGameTurn( game, pl, true );
                }
                else
                    srv.messageToPlayer( c, gname, pl == null ? -1 : pl.getPlayerNumber(), /*I*/"You can't end your turn yet."/*18N*/ );
            }
            else
            {
                srv.messageToPlayerKeyed( c, gname, SOCServer.PN_REPLY_TO_UNDETERMINED, "base.reply.not.your.turn" );  // "It's not your turn."
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught at handleENDTURN" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * Handle the "simple request" message.
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection
     * @param simpleRequestMsg  the message
     * @since 1.1.18
     */
    private void handleSIMPLEREQUEST( SOCGame game, Connection c, final SOCSimpleRequest simpleRequestMsg )
    {
        final String gameName = game.getName();
        SOCPlayer clientPl = game.getPlayer( c.getData() );
        if (clientPl == null)
            return;

        final int pn = simpleRequestMsg.getPlayerNumber();
        final boolean clientIsPN = (pn == clientPl.getPlayerNumber());  // probably required for most request types
        final int reqtype = simpleRequestMsg.getRequestType();
        final int cpn = game.getCurrentPlayerNumber();

        boolean replyDecline = false;  // if true, reply with generic decline (pn = -1, reqtype, 0, 0)

        switch (reqtype)
        {
        case SOCSimpleRequest.SC_PIRI_FORT_ATTACK:
        {
            final SOCShip adjac = game.canAttackPirateFortress();
            if ((!clientIsPN) || (pn != cpn) || (adjac == null) || (adjac.getPlayerNumber() != cpn))
            {
                srv.messageToPlayer( c, gameName, pn, new SOCSimpleRequest( gameName, -1, reqtype, 0, 0 ) );
                return;  // <--- early return: deny ---
            }

            final GameState prevState = game.getGameState();
            final SOCPlayer cp = game.getPlayer( cpn );
            final int prevNumWarships = cp.getNumWarships();  // in case some are lost, we'll announce that
            final SOCFortress fort = cp.getFortress();

            final int[] res = game.attackPirateFortress( adjac );

            if (res.length > 1)
            {
                // lost 1 or 2 ships adjacent to fortress.  res[1] == adjac.coordinate

                srv.messageToGame( gameName, true, new SOCRemovePiece( gameName, adjac ) );
                if (res.length > 2)
                    srv.messageToGame( gameName, true, new SOCRemovePiece( gameName, cpn, SOCPlayingPiece.SHIP, res[2] ) );

                final int n = cp.getNumWarships();
                if (n != prevNumWarships)
                    srv.messageToGame( gameName, true, new SOCPlayerElement
                        ( gameName, cpn, SOCPlayerElement.SET, PEType.SCENARIO_WARSHIP_COUNT, n ) );
            }
            else
            {
                // player won battle

                final int fortStrength = fort.getStrength();
                srv.messageToGame( gameName, true, new SOCPieceValue
                    ( gameName, SOCPlayingPiece.FORTRESS, fort.getCoordinates(), fortStrength, 0 ) );
                if (0 == fortStrength)
                    srv.messageToGame( gameName, true, new SOCPutPiece
                        ( gameName, cpn, SOCPlayingPiece.SETTLEMENT, fort.getCoordinates() ) );
            }

            srv.messageToGame( gameName, true, new SOCSimpleAction
                ( gameName, cpn, SOCSimpleAction.SC_PIRI_FORT_ATTACK_RESULT, res[0], res.length - 1 ) );

            // check for end of player's turn
            if (!handler.checkTurn( c, game ))
            {
                handler.endGameTurn( game, cp, false );
            }
            else
            {
                // still player's turn, even if they won
                final GameState gstate = game.getGameState();
                if (gstate != prevState)
                    handler.sendGameState( game );  // might be GAME_OVER, if player won
            }
        }
        break;

        case SOCSimpleRequest.TRADE_PORT_PLACE:
        {
            if (clientIsPN && (pn == cpn))
            {
                final int edge = simpleRequestMsg.getValue1();
                if ((game.getGameState() == PLACING_INV_ITEM) && game.canPlacePort( clientPl, edge ))
                {
                    final int ptype = game.placePort( edge );

                    handler.sendGameState( game );  // PLAY1 or SPECIAL_BUILDING
                    srv.messageToGame( gameName, true, new SOCSimpleRequest
                        ( gameName, cpn, SOCSimpleRequest.TRADE_PORT_PLACE, edge, ptype ) );
                }
                else
                {
                    replyDecline = true;  // client will print a text message, no need to send one
                }
            }
            else
            {
                srv.messageToPlayerKeyed( c, gameName, pn, "base.reply.not.your.turn" );
                replyDecline = true;
            }
        }
        break;

        default:
            // deny unknown types
            replyDecline = true;
            System.err.println
                ( "handleSIMPLEREQUEST: Unknown type " + reqtype + " from " + c.getData() + " in game " + game );
        }

        if (replyDecline)
            srv.messageToPlayer( c, gameName, pn, new SOCSimpleRequest( gameName, -1, reqtype, 0, 0 ) );
    }


    /// Player trades and bank trades ///
    /**
     * handle "make offer" message.
     * Calls {@link SOCGameHandler#sendTradeOffer(SOCPlayer, Connection)}.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param mes  the message
     * @since 1.0.0
     */
    private void handleMAKEOFFER( SOCGame game, Connection c, final SOCMakeOffer mes )
    {
        final String gameName = game.getName();
        if (game.isGameOptionSet( "NT" ))
        {
            // Check here as a fallback;
            // client should know to not show trade-offer UI when game has option NT
            srv.messageToPlayer( c, gameName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                "Trading is not allowed in this game." );  // i18n OK: is fallback only

            return;  // <---- Early return: No Trading ----
        }

        final SOCTradeOffer offer = mes.getOffer();
        final SOCPlayer player = game.getPlayer( c.getData() );
        if (player == null)
            return;

        try
        {
            game.takeMonitor();

            // Check offer contents as a fallback to client checks:
            SOCResourceSet giveSet = offer.getGiveSet();
            boolean canOffer = player.getResources().contains( giveSet );
            final int cpn = game.getCurrentPlayerNumber();
            if (canOffer && (cpn != player.getPlayerNumber()))
            {
                boolean[] to = offer.getTo();
                for (int pn = 0; pn < to.length; ++pn)
                {
                    if (to[pn] && (pn != cpn))
                    {
                        canOffer = false;
                        break;
                    }
                }
            }

            if (!canOffer)
            {
                final int seatNumber = player.getPlayerNumber();
                SOCMessage msg = (c.getRemoteVersion() >= SOCRejectOffer.VERSION_FOR_REPLY_REASONS)
                    ? new SOCRejectOffer(gameName, seatNumber, SOCRejectOffer.REASON_CANNOT_MAKE_OFFER)
                    : new SOCGameServerText( gameName, "You can't make that offer." );  // i18n OK: is fallback only
                srv.messageToPlayer(c, gameName, seatNumber, msg);
                return;  // <---- Early return: Can't offer that ----
            }

            /**
             * remake the offer with data that we know is accurate,
             * such as 'from' field.
             * set and announce the offer, including text message similar to bank/port trade.
             */
            final SOCTradeOffer remadeOffer = new SOCTradeOffer
                ( gameName, player.getPlayerNumber(), offer.getTo(), giveSet, offer.getGetSet() );
            player.setCurrentOffer( remadeOffer );

            handler.sendTradeOffer( player, null );
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "clear offer" message.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param clearOfferMessage  the message
     * @since 1.0.0
     */
    private void handleCLEAROFFER( SOCGame game, Connection c, @SuppressWarnings("unused") SOCClearOffer clearOfferMessage )
    {
        game.takeMonitor();

        try
        {
            final String gameName = game.getName();
            final SOCPlayer player = game.getPlayer( c.getData() );
            if (player == null)
                return;

            player.setCurrentOffer( null );
            srv.messageToGame( gameName, true, new SOCClearOffer( gameName, player.getPlayerNumber() ) );

            /**
             * clear all the trade messages
             */
            srv.gameList.takeMonitorForGame( gameName );
            try
            {
                if (game.clientVersionLowest >= SOCClearTradeMsg.VERSION_FOR_CLEAR_ALL)
                {
                    srv.messageToGameWithMon( gameName, true, new SOCClearTradeMsg( gameName, -1 ) );
                }
                else
                {
                    for (int i = 0; i < game.maxPlayers; i++)
                        srv.messageToGameWithMon( gameName, false, new SOCClearTradeMsg( gameName, i ) );

                    if (srv.recordGameEventsIsActive())
                        srv.recordGameEvent( gameName, new SOCClearTradeMsg( gameName, -1 ) );
                }
            }
            finally
            {
                srv.gameList.releaseMonitorForGame( gameName );
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "reject offer" message.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param rejectOfferMessage  the message
     * @since 1.0.0
     */
    private void handleREJECTOFFER( SOCGame game, Connection c, @SuppressWarnings("unused") SOCRejectOffer rejectOfferMessage )
    {
        SOCPlayer player = game.getPlayer( c.getData() );
        if (player == null)
            return;
        final int pn = player.getPlayerNumber();

        try
        {
            game.takeMonitor();
            game.rejectTradeOffersTo( pn );
        }
        finally
        {
            game.releaseMonitor();
        }

        srv.messageToGame( game.getName(), true, new SOCRejectOffer( game.getName(), pn ));
    }

    /**
     * handle "accept offer" message.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param mes  the message
     * @since 1.0.0
     */
    private void handleACCEPTOFFER( final SOCGame game, final Connection c, final SOCAcceptOffer mes )
    {
        SOCPlayer player = game.getPlayer( c.getData() );
        if (player == null)
            return;

        executeTrade( game, mes.getOfferingNumber(), player.getPlayerNumber(), c );
    }

    /**
     * Check and complete a player trade accepted by both sides, and announce it with messages to the game.
     * Calls {@link SOCGame#canMakeTrade(int, int)}, {@link SOCGame#makeTrade(int, int)},
     * {@link SOCGameHandler#reportTrade(SOCGame, int, int)}, then clears all trade offers
     * by announcing {@link SOCClearOffer}.
     *<P>
     * If trade cannot be made, will send {@code acceptingNumber}'s client a message explaining that.
     *<P>
     * <B>Note:</B> Calling this method assumes the players have either accepted and/or made a counter-offer,
     * and that the offer-initiating player's {@link SOCPlayer#getCurrentOffer()} is set to the trade to be executed.
     *
     * @param game the game instance to execute the trade in
     * @param offeringNumber  Player number offering the trade
     * @param acceptingNumber  Player number accepting the trade
     * @param connection  accepting player client's connection, if needed to reply that trade is not possible
     * @since 2.4.50
     */
    private void executeTrade( final SOCGame game, final int offeringNumber,
        final int acceptingNumber, final Connection connection )
    {
        game.takeMonitor();

        try
        {
            final String gameName = game.getName();

            if (game.canMakeTrade( offeringNumber, acceptingNumber ))
            {
                game.makeTrade( offeringNumber, acceptingNumber );
                handler.reportTrade( game, offeringNumber, acceptingNumber );

                /**
                 * clear all offers
                 */
                for (int i = 0; i < game.maxPlayers; i++)
                    game.getPlayer( i ).setCurrentOffer( null );

                try
                {
                    srv.gameList.takeMonitorForGame( gameName );
                    if (game.clientVersionLowest >= SOCClearOffer.VERSION_FOR_CLEAR_ALL)
                    {
                        srv.messageToGameWithMon( gameName, true, new SOCClearOffer( gameName, -1 ) );
                    }
                    else
                    {
                        for (int i = 0; i < game.maxPlayers; i++)
                            srv.messageToGameWithMon( gameName, false, new SOCClearOffer( gameName, i ) );

                        if (srv.recordGameEventsIsActive())
                            srv.recordGameEvent( gameName, new SOCClearOffer( gameName, -1 ) );
                    }
                }
                finally
                {
                    srv.gameList.releaseMonitorForGame( gameName );
                }
            }
            else
            {
                if (connection.getRemoteVersion() >= SOCRejectOffer.VERSION_FOR_REPLY_REASONS)
                {
                    srv.messageToPlayer( connection, gameName, acceptingNumber,
                        new SOCRejectOffer( gameName, acceptingNumber, SOCRejectOffer.REASON_CANNOT_MAKE_TRADE ));
                }
                else
                    srv.messageToPlayerKeyed( connection, gameName, acceptingNumber,
                        "reply.common.trade.cannot_make");  // "You can't make that trade."
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "bank trade" message.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param connection  the connection that sent the message
     * @param mes  the message
     * @since 1.0.0
     */
    private void handleBANKTRADE( SOCGame game, Connection connection, final SOCBankTrade mes )
    {
        final String gaName = game.getName();
        final SOCResourceSet give = mes.getGiveSet(),
            get = mes.getGetSet();

        game.takeMonitor();

        try
        {
            if (handler.checkTurn(connection, game))
            {
                if (game.canMakeBankTrade(give, get))
                {
                    game.makeBankTrade( give, get );
                    handler.reportBankTrade( game, give, get);
                }
                else
                {
                    final int pn = game.getCurrentPlayerNumber();
                    if (connection.getRemoteVersion() >= SOCRejectOffer.VERSION_FOR_REPLY_REASONS)
                    {
                        srv.messageToPlayer( connection, gaName, pn,
                            new SOCRejectOffer( gaName, -1, SOCRejectOffer.REASON_CANNOT_MAKE_TRADE ) );
                    }
                    else
                        srv.messageToPlayerKeyed( connection, gaName, pn,
                            "reply.common.trade.cannot_make");  // "You can't make that trade."

                    SOCClientData scd = (SOCClientData) connection.getAppData();
                    if ((scd != null) && scd.isRobot)
                    {
                        SOCResourceSet rsc = game.getPlayer( pn ).getResources();

                        D.ebugPrintlnINFO(String.format( "[%s] ILLEGAL BANK TRADE: %s: "
                            + "has %s  offered: %s, gets %s", Thread.currentThread().getName(),
                            connection.getData(), rsc.toString(), give.toString(), get.toString() ));
                    }
                }
            }
            else
            {
                if (connection.getRemoteVersion() >= SOCRejectOffer.VERSION_FOR_REPLY_REASONS)
                {
                    srv.messageToPlayer( connection, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                        new SOCRejectOffer( gaName, -1, SOCRejectOffer.REASON_NOT_YOUR_TURN ));
                }
                else
                {
                    srv.messageToPlayerKeyed( connection, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                        "base.reply.not.your.turn" );  // "It's not your turn."
                }
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }


    /// Game piece building, placement, and moving ///


    /**
     * handle "build request" message.
     * If client is current player, they want to buy a {@link SOCPlayingPiece}.
     * Otherwise, if 6-player board, they are requesting to build during the
     * next {@link SOCGame#SPECIAL_BUILDING Special Building Phase}.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param buildRequestMsg  the message
     * @since 1.0.0
     * @see #handleBUILDREQUEST(SOCGame, SOCPlayer, Connection, int, boolean)
     */
    private void handleBUILDREQUEST( SOCGame ga, Connection c, final SOCBuildRequest buildRequestMsg )
    {
        final String gaName = ga.getName();
        ga.takeMonitor();

        try
        {
            final boolean isCurrent = handler.checkTurn( c, ga );
            SOCPlayer player = ga.getPlayer( c.getData() );
            final int pn = player.getPlayerNumber();
            final int pieceType = buildRequestMsg.getPieceType();
            boolean sendDenyReply = false;  // for robots' benefit

            if (isCurrent)
            {
                if ((ga.getGameState() == PLAY1) || (ga.getGameState() == SPECIAL_BUILDING))
                {
                    sendDenyReply = !handleBUILDREQUEST( ga, player, c, pieceType, true );
                }
                else if (pieceType == -1)
                {
                    // 6-player board: Special Building Phase during start of own turn
                    try
                    {
                        ga.askSpecialBuild( pn, true );
                        srv.messageToGame
                            ( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET, PEType.ASK_SPECIAL_BUILD, 1 ) );
                        handler.endGameTurn( ga, player, true );  // triggers start of SBP
                    }
                    catch( NoSuchElementException e )
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.special.PLP.common" );
                        // "House rule: Special Building phase requires 5 or 6 players."
                        sendDenyReply = true;
                    }
                    catch( IllegalStateException e )
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.now.ask" );  // "You can't ask to build now."
                        sendDenyReply = true;
                    }
                }
                else
                {
                    srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.now" );  // "You can't build now."
                    sendDenyReply = true;
                }
            }
            else
            {
                if (ga.maxPlayers <= 4)
                {
                    srv.messageToPlayerKeyed( c, gaName, pn, "base.reply.not.your.turn" );  // "It's not your turn."
                    sendDenyReply = true;
                }
                else
                {
                    // 6-player board: Special Building Phase
                    // during other player's turn
                    try
                    {
                        ga.askSpecialBuild( pn, true );  // will validate that they can build now
                        srv.messageToGame
                            ( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET, PEType.ASK_SPECIAL_BUILD, 1 ) );
                    }
                    catch( NoSuchElementException e )
                    {
                        srv.messageToPlayerKeyed
                            ( c, gaName, pn, "action.build.cannot.special.PLP.common" );
                        // "House rule: Special Building phase requires 5 or 6 players."
                        sendDenyReply = true;
                    }
                    catch( IllegalStateException e )
                    {
                        srv.messageToPlayerKeyed
                            ( c, gaName, pn, "action.build.cannot.now.ask" );  // "You can't ask to build now."
                        sendDenyReply = true;
                    }
                }
            }

            if (sendDenyReply && ga.getPlayer( pn ).isRobot())
            {
                srv.messageToPlayer( c, gaName, pn, new SOCCancelBuildRequest( gaName, pieceType ) );
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught at handleBUILDREQUEST" );
        }
        finally
        {
            ga.releaseMonitor();
        }
    }

    /**
     * Handle a client player's request to buy a type of playing piece,
     * for {@link #handleBUILDREQUEST(SOCGame, Connection, SOCBuildRequest)}
     * and {@link #handlePUTPIECE(SOCGame, Connection, SOCPutPiece)}:
     * Checks player piece counts and resources, buys the piece in game,
     * announces {@link SOCPlayerElement} messages for the resources spent,
     * optionally announces new game state to game's members.
     * If player can't buy, tells them that in a server text.
     *<P>
     * <B>Locks and preconditions:</B>
     *<UL>
     * <LI> Caller already called {@link SOCGame#takeMonitor() ga.takeMonitor()}
     * <LI> {@code player} already checked to be current player
     * <LI> {@link SOCGame#getGameState() ga.getGameState()} is either
     *      {@link SOCGame#PLAY1} or {@link SOCGame#SPECIAL_BUILDING}
     *</UL>
     * @param game the SOCGame instance that this message is addressed to
     * @param player  Requesting player; must be current player
     * @param c   Requesting {@code player}'s connection
     * @param pieceType {@link SOCPlayingPiece#SETTLEMENT}, {@link SOCPlayingPiece#SHIP}, etc
     * @param sendGameState  True if {@link SOCGameHander#sendGameState(SOCGame)} should be called
     *            after buying the piece
     * @return True if piece build was allowed, false if it was rejected.<BR>
     *   If false, game state is unchanged. If true, it's a state like {@link SOCGame#PLACING_ROAD}
     *   after being changed here by a method like {@link SOCGame#buyRoad(int)}.
     * @since 2.0.00
     */
    private boolean handleBUILDREQUEST( final SOCGame game, final SOCPlayer player,
        final Connection c, final int pieceType, final boolean sendGameState )
    {
        final String gameName = game.getName();
        final int pn = player.getPlayerNumber();

        final boolean usePlayerElements = (game.clientVersionLowest >= SOCPlayerElements.MIN_VERSION);
        boolean sendDenyReply = false;

        switch (pieceType)
        {
        case SOCPlayingPiece.ROAD:

            if (game.couldBuildRoad( pn ))
            {
                game.buyRoad( pn );
                if (usePlayerElements)
                {
                    srv.messageToGame( gameName, true, new SOCPlayerElements
                        ( gameName, pn, SOCPlayerElement.LOSE, SOCRoad.COST ) );
                }
                else
                {
                    srv.messageToGame( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.CLAY, 1 ) );
                    srv.messageToGame( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.WOOD, 1 ) );

                    if (srv.recordGameEventsIsActive())
                        srv.recordGameEvent( gameName, new SOCPlayerElements
                            ( gameName, pn, SOCPlayerElement.LOSE, SOCRoad.COST ) );
                }
                if (sendGameState)
                    handler.sendGameState( game );
            }
            else
            {
                srv.messageToPlayerKeyed
                    ( c, gameName, pn, "action.build.cannot.now.road" );  // "You can't build a road now."
                sendDenyReply = true;
            }
            break;

        case SOCPlayingPiece.SETTLEMENT:

            if (game.couldBuildSettlement( pn ))
            {
                game.buySettlement( pn );
                if (usePlayerElements)
                {
                    srv.messageToGame( gameName, true, new SOCPlayerElements
                        ( gameName, pn, SOCPlayerElement.LOSE, SOCSettlement.COST ) );
                }
                else
                {
                    srv.gameList.takeMonitorForGame( gameName );
                    srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.CLAY, 1 ) );
                    srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.SHEEP, 1 ) );
                    srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.WHEAT, 1 ) );
                    srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.WOOD, 1 ) );
                    srv.gameList.releaseMonitorForGame( gameName );

                    if (srv.recordGameEventsIsActive())
                        srv.recordGameEvent( gameName, new SOCPlayerElements
                            ( gameName, pn, SOCPlayerElement.LOSE, SOCSettlement.COST ) );
                }
                if (sendGameState)
                    handler.sendGameState( game );
            }
            else
            {
                srv.messageToPlayerKeyed
                    ( c, gameName, pn, "action.build.cannot.now.stlmt" );  // "You can't build a settlement now."
                sendDenyReply = true;
            }
            break;

        case SOCPlayingPiece.CITY:

            if (game.couldBuildCity( pn ))
            {
                game.buyCity( pn );
                if (usePlayerElements)
                {
                    srv.messageToGame( gameName, true, new SOCPlayerElements
                        ( gameName, pn, SOCPlayerElement.LOSE, SOCCity.COST ) );
                }
                else
                {
                    srv.messageToGame( game.getName(), false, new SOCPlayerElement
                        ( game.getName(), pn, SOCPlayerElement.LOSE, PEType.ORE, 3 ) );
                    srv.messageToGame( game.getName(), false, new SOCPlayerElement
                        ( game.getName(), pn, SOCPlayerElement.LOSE, PEType.WHEAT, 2 ) );

                    if (srv.recordGameEventsIsActive())
                        srv.recordGameEvent( gameName, new SOCPlayerElements
                            ( gameName, pn, SOCPlayerElement.LOSE, SOCCity.COST ) );
                }
                if (sendGameState)
                    handler.sendGameState( game );
            }
            else
            {   // "You can't build a city now."
                srv.messageToPlayerKeyed( c, gameName, pn, "action.build.cannot.now.city" );
                sendDenyReply = true;
            }
            break;

        case SOCPlayingPiece.SHIP:

            if (game.couldBuildShip( pn ))
            {
                game.buyShip( pn );
                if (usePlayerElements)
                {
                    srv.messageToGame( gameName, true, new SOCPlayerElements
                        ( gameName, pn, SOCPlayerElement.LOSE, SOCShip.COST ) );
                }
                else
                {
                    srv.messageToGame( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.SHEEP, 1 ) );
                    srv.messageToGame( gameName, false, new SOCPlayerElement
                        ( gameName, pn, SOCPlayerElement.LOSE, PEType.WOOD, 1 ) );

                    if (srv.recordGameEventsIsActive())
                        srv.recordGameEvent( gameName, new SOCPlayerElements
                            ( gameName, pn, SOCPlayerElement.LOSE, SOCShip.COST ) );
                }
                if (sendGameState)
                    handler.sendGameState( game );
            }
            else
            {   // "You can't build a ship now."
                srv.messageToPlayerKeyed( c, gameName, pn, "action.build.cannot.now.ship" );
                sendDenyReply = true;
            }
            break;

        default:    // "Unknown piece type."
            srv.messageToPlayerKeyed( c, gameName, pn, "reply.piece.type.unknown" );
            sendDenyReply = true;
        }

        return !sendDenyReply;
    }

    /**
     * handle "cancel build request" message.
     * Cancel placement and send new game state, if cancel is allowed.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param mes  the message
     * @since 1.0.0
     */
    private void handleCANCELBUILDREQUEST( SOCGame game, Connection c, final SOCCancelBuildRequest mes )
    {
        game.takeMonitor();

        try
        {
            final String gameName = game.getName();
            if (handler.checkTurn( c, game ))
            {
                final SOCPlayer player = game.getPlayer( c.getData() );
                final int pn = player.getPlayerNumber();
                final GameState gstate = game.getGameState();
                final boolean usePlayerElements = (game.clientVersionLowest >= SOCPlayerElements.MIN_VERSION);

                boolean noAction = false;  // If true, there was nothing cancelable: Don't call handler.sendGameState

                switch (mes.getPieceType())
                {
                case SOCPlayingPiece.ROAD:

                    if (   (gstate == PLACING_ROAD)
                        || (gstate == PLACING_FREE_ROAD2))
                    {
                        game.cancelBuildRoad( pn );
                        if (gstate == PLACING_ROAD)
                        {
                            if (usePlayerElements)
                            {
                                srv.messageToGame( gameName, true, new SOCPlayerElements
                                    ( gameName, pn, SOCPlayerElement.GAIN, SOCRoad.COST ) );
                            }
                            else
                            {
                                srv.messageToGame( gameName, false, new SOCPlayerElement
                                    ( gameName, pn, SOCPlayerElement.GAIN, PEType.CLAY, 1 ) );
                                srv.messageToGame( gameName, false, new SOCPlayerElement
                                    ( gameName, pn, SOCPlayerElement.GAIN, PEType.WOOD, 1 ) );

                                if (srv.recordGameEventsIsActive())
                                    srv.recordGameEvent( gameName, new SOCPlayerElements
                                        ( gameName, pn, SOCPlayerElement.GAIN, SOCRoad.COST ) );
                            }
                        }
                        else
                        {
                            srv.messageToGameKeyed( game, true, true, "action.card.roadbuilding.skip.r", player.getName() );
                            // "{0} skipped placing the second road."
                        }
                    }
                    else
                    {
                        srv.messageToPlayer( c, gameName, pn, /*I*/"You didn't buy a road."/*18N*/ );
                        noAction = true;
                    }
                    break;

                case SOCPlayingPiece.SETTLEMENT:

                    if (gstate == PLACING_SETTLEMENT)
                    {
                        game.cancelBuildSettlement( pn );
                        if (usePlayerElements)
                        {
                            srv.messageToGame( gameName, true, new SOCPlayerElements
                                ( gameName, pn, SOCPlayerElement.GAIN, SOCSettlement.COST ) );
                        }
                        else
                        {
                            srv.gameList.takeMonitorForGame( gameName );
                            srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.GAIN, PEType.CLAY, 1 ) );
                            srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.GAIN, PEType.SHEEP, 1 ) );
                            srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.GAIN, PEType.WHEAT, 1 ) );
                            srv.messageToGameWithMon( gameName, false, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.GAIN, PEType.WOOD, 1 ) );
                            srv.gameList.releaseMonitorForGame( gameName );

                            if (srv.recordGameEventsIsActive())
                                srv.recordGameEvent( gameName, new SOCPlayerElements
                                    ( gameName, pn, SOCPlayerElement.GAIN, SOCSettlement.COST ) );

                        }
                    }
                    else if ((gstate == START1B) || (gstate == START2B) || (gstate == START3B))
                    {
                        SOCSettlement pp = new SOCSettlement( player, player.getLastSettlementCoord(), null );
                        game.undoPutInitSettlement( pp );
                        srv.messageToGame( gameName, true, mes );  // Re-send to all clients to announce it
                        // (Safe since we've validated all message parameters)
                        srv.messageToGameKeyed( game, true, true, "action.built.stlmt.cancel", player.getName() );
                        //  "{0} cancelled this settlement placement."
                        // The handler.sendGameState below is redundant if client reaction changes game state
                    }
                    else
                    {
                        srv.messageToPlayer( c, gameName, pn, /*I*/"You didn't buy a settlement."/*18N*/ );
                        noAction = true;
                    }
                    break;

                case SOCPlayingPiece.CITY:

                    if (gstate == PLACING_CITY)
                    {
                        game.cancelBuildCity( pn );
                        if (usePlayerElements)
                        {
                            srv.messageToGame( gameName, true, new SOCPlayerElements
                                ( gameName, pn, SOCPlayerElement.GAIN, SOCCity.COST ) );
                        }
                        else
                        {
                            srv.messageToGame( gameName, false, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.GAIN, PEType.ORE, 3 ) );
                            srv.messageToGame( gameName, false, new SOCPlayerElement
                                ( gameName, pn, SOCPlayerElement.GAIN, PEType.WHEAT, 2 ) );


                            if (srv.recordGameEventsIsActive())
                                srv.recordGameEvent( gameName, new SOCPlayerElements
                                    ( gameName, pn, SOCPlayerElement.GAIN, SOCCity.COST ) );
                        }
                    }
                    else
                    {
                        srv.messageToPlayer( c, gameName, pn, /*I*/"You didn't buy a city."/*18N*/ );
                        noAction = true;
                    }
                    break;

                case SOCPlayingPiece.SHIP:

                    if ((gstate == PLACING_SHIP) || (gstate == PLACING_FREE_ROAD2))
                    {
                        game.cancelBuildShip( pn );
                        if (gstate == PLACING_SHIP)
                        {
                            if (usePlayerElements)
                            {
                                srv.messageToGame( gameName, true, new SOCPlayerElements
                                    ( gameName, pn, SOCPlayerElement.GAIN, SOCShip.COST ) );
                            }
                            else
                            {
                                srv.messageToGame( gameName, false, new SOCPlayerElement
                                    ( gameName, pn, SOCPlayerElement.GAIN, PEType.SHEEP, 1 ) );
                                srv.messageToGame( gameName, false, new SOCPlayerElement
                                    ( gameName, pn, SOCPlayerElement.GAIN, PEType.WOOD, 1 ) );

                                if (srv.recordGameEventsIsActive())
                                    srv.recordGameEvent( gameName, new SOCPlayerElements
                                        ( gameName, pn, SOCPlayerElement.GAIN, SOCShip.COST ) );
                            }
                        }
                        else
                        {
                            srv.messageToGameKeyed( game, true, true, "action.card.roadbuilding.skip.s", player.getName() );
                            // "{0} skipped placing the second ship."
                        }
                    }
                    else
                    {
                        srv.messageToPlayer( c, gameName, pn, /*I*/"You didn't buy a ship."/*18N*/ );
                        noAction = true;
                    }
                    break;

                case SOCCancelBuildRequest.INV_ITEM_PLACE_CANCEL:
                    SOCInventoryItem item = null;
                    if (gstate == PLACING_INV_ITEM)
                        item = game.cancelPlaceInventoryItem( false );

                    if (item != null)
                        srv.messageToGame( gameName, true, new SOCInventoryItemAction
                            ( gameName, pn, SOCInventoryItemAction.ADD_PLAYABLE, item.itype,
                                item.isKept(), item.isVPItem(), item.canCancelPlay ) );

                    if ((item != null) || (gstate != game.getGameState()))
                    {
                        srv.messageToGameKeyed( game, true, true, "reply.placeitem.cancel", player.getName() );
                        // "{0} canceled placement of a special item."
                    }
                    else
                    {
                        srv.messageToPlayerKeyed( c, gameName, pn, "reply.placeitem.cancel.cannot" );
                        // "Cannot cancel item placement."
                        noAction = true;
                    }
                    break;

                default:
                    throw new IllegalArgumentException( "Unknown piece type " + mes.getPieceType() );
                }

                if (!noAction)
                {
                    handler.sendGameState( game );
                }
                else
                {
                    // bot is waiting for a gamestate reply, not text
                    final SOCClientData scd = (SOCClientData) c.getAppData();
                    if ((scd != null) && scd.isRobot)
                        srv.messageToPlayer( c, gameName, pn, new SOCGameState( gameName, gstate ) );
                }
            }
            else
            {
                srv.messageToPlayerKeyed
                    ( c, gameName, SOCServer.PN_REPLY_TO_UNDETERMINED, "base.reply.not.your.turn" );  // "It's not your turn."
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "put piece" message.
     *<P>
     * Because the current player changes during initial placement,
     * this method has a simplified version of some of the logic from
     * {@link SOCGameHandler#endGameTurn(SOCGame, SOCPlayer, boolean)}
     * to detect and announce the new turn.
     *
     * @param game the game where this piece is being placed.
     * @param c  the connection that sent the message
     * @param putPieceMessage  the message
     * @since 1.0.0
     */
    private void handlePUTPIECE( SOCGame game, Connection c, SOCPutPiece putPieceMessage )
    {
        game.takeMonitor();

        try
        {
            final String gaName = game.getName();
            final String plName = c.getData();
            SOCPlayer player = game.getPlayer( plName );

            /**
             * make sure is current player and can place now, then do so
             */
            if (handler.checkTurn( c, game ))
            {
                boolean sendDenyReply = false;
                /*
                   if (D.ebugOn) {
                   D.ebugPrintln("BEFORE");
                   for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                   SOCPlayer tmpPlayer = ga.getPlayer(pn);
                   D.ebugPrintln("Player # "+pn);
                   for (int i = 0x22; i < 0xCC; i++) {
                   if (tmpPlayer.isPotentialRoad(i))
                   D.ebugPrintln("### POTENTIAL ROAD AT "+Integer.toHexString(i));
                   }
                   }
                   }
                 */

                GameState gameState = game.getGameState();
                final int coord = putPieceMessage.getCoordinates();
                final int pieceType = putPieceMessage.getPieceType();
                final int pn = player.getPlayerNumber();
                final boolean isBuyAndPut = (gameState == PLAY1) || (gameState == SPECIAL_BUILDING);
                final SOCPlayer longestRoutePlayer = game.getPlayerWithLongestRoad();

                if (isBuyAndPut)
                {
                    // Handle combined buildrequest + putpiece message: (client v2.0.00 or newer)

                    if (!handleBUILDREQUEST( game, player, c, pieceType, false ))
                    {
                        return;  // <--- Can't build right now ---
                        // will call ga.releaseMonitor() in finally-block before returning
                    }

                    gameState = game.getGameState();  // updated by handleBUILDREQUEST
                }

                switch (pieceType)
                {
                case SOCPlayingPiece.ROAD:

                    if ((gameState == START1B) || (gameState == START2B) || (gameState == START3B)
                        || (gameState == PLACING_ROAD)
                        || (gameState == PLACING_FREE_ROAD1) || (gameState == PLACING_FREE_ROAD2))
                    {
                        if (player.isPotentialRoad( coord ) && (player.getNumPieces( SOCPlayingPiece.ROAD ) >= 1))
                        {
                            final SOCRoad rd = new SOCRoad( player, coord, null );
                            game.putPiece( rd );  // Changes game state and (if initial placement) player

                            // If placing this piece reveals a fog hex, putPiece will call srv.gameEvent
                            // which will send a SOCRevealFogHex message to the game.

                            /*
                               if (D.ebugOn) {
                               D.ebugPrintln("AFTER");
                               for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                               SOCPlayer tmpPlayer = ga.getPlayer(pn);
                               D.ebugPrintln("Player # "+pn);
                               for (int i = 0x22; i < 0xCC; i++) {
                               if (tmpPlayer.isPotentialRoad(i))
                               D.ebugPrintln("### POTENTIAL ROAD AT "+Integer.toHexString(i));
                               }
                               }
                               }
                             */

                            // TODO refactor common netcode here with other piece types

                            srv.gameList.takeMonitorForGame( gaName );
                            try
                            {
                                srv.messageToGameKeyed
                                    ( game, true, false, "action.built.road", plName );  // "Joe built a road."
                                srv.messageToGameWithMon
                                    ( gaName, true, new SOCPutPiece( gaName, pn, SOCPlayingPiece.ROAD, coord ) );
                                handler.reportLongestRoadIfChanged( game, longestRoutePlayer, true );
                                if (!game.pendingMessagesOut.isEmpty())
                                    handler.sendGamePendingMessages( game, false );
                            }
                            finally
                            {
                                srv.gameList.releaseMonitorForGame( gaName );
                            }

                            // If needed, call sendTurn or send SOCRollDicePrompt
                            handler.sendTurnStateAtInitialPlacement( game, player, c, gameState );

                            GameState newState = game.getGameState();
                            if (   (newState == STARTS_WAITING_FOR_PICK_GOLD_RESOURCE)
                                || (newState == WAITING_FOR_PICK_GOLD_RESOURCE))
                            {
                                // gold hex revealed from fog (scenario SC_FOG)
                                handler.sendGameState_sendGoldPickAnnounceText( game, gaName, c, null );
                            }
                        }
                        else
                        {
                            D.ebugPrintlnINFO( "ILLEGAL ROAD: 0x" + Integer.toHexString( coord )
                                + ": player " + pn );
                            if (player.isRobot() && D.ebugOn)
                            {
                                D.ebugPrintlnINFO( " - pl.isPotentialRoad: " + player.isPotentialRoad( coord ) );
                                SOCPlayingPiece pp = game.getBoard().roadOrShipAtEdge( coord );
                                D.ebugPrintlnINFO( " - roadAtEdge: " + ((pp != null) ? pp : "none") );
                            }

                            srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.there.road" );
                            // "You can't build a road there."
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.now.road" );
                        // "You can't build a road now."
                    }
                    break;

                case SOCPlayingPiece.SETTLEMENT:

                    if ((gameState == START1A) || (gameState == START2A)
                        || (gameState == START3A) || (gameState == PLACING_SETTLEMENT))
                    {
                        if (player.canPlaceSettlement( coord ) && (player.getNumPieces( SOCPlayingPiece.SETTLEMENT ) >= 1))
                        {
                            final SOCSettlement se = new SOCSettlement( player, coord, null );
                            game.putPiece( se );   // Changes game state and (if initial placement) player

                            // TODO refactor common netcode here with other piece types

                            srv.gameList.takeMonitorForGame( gaName );
                            try
                            {
                                srv.messageToGameKeyed
                                    ( game, true, false, "action.built.stlmt", plName );  // "Joe built a settlement."
                                srv.messageToGameWithMon
                                    ( gaName, true, new SOCPutPiece( gaName, pn, SOCPlayingPiece.SETTLEMENT, coord ) );
                                handler.reportLongestRoadIfChanged( game, longestRoutePlayer, true );
                                if (!game.pendingMessagesOut.isEmpty())
                                    handler.sendGamePendingMessages( game, false );
                            }
                            finally
                            {
                                srv.gameList.releaseMonitorForGame( gaName );
                            }

                            // Check player and send new game state
                            if (!handler.checkTurn( c, game ))
                                handler.sendTurn( game, false );  // Announce new state and new current player
                            else
                                handler.sendGameState( game );

                            if (game.hasSeaBoard && (game.getGameState() == STARTS_WAITING_FOR_PICK_GOLD_RESOURCE))
                            {
                                // Prompt to pick from gold: send text and SOCSimpleRequest(PROMPT_PICK_RESOURCES)
                                handler.sendGameState_sendGoldPickAnnounceText( game, gaName, c, null );
                            }
                        }
                        else
                        {
                            D.ebugPrintlnINFO( "ILLEGAL SETTLEMENT: 0x" + Integer.toHexString( coord )
                                + ": player " + pn );
                            if (player.isRobot() && D.ebugOn)
                            {
                                D.ebugPrintlnINFO( " - pl.isPotentialSettlement: "
                                    + player.isPotentialSettlement( coord ) );
                                SOCPlayingPiece pp = game.getBoard().settlementAtNode( coord );
                                D.ebugPrintlnINFO( " - settlementAtNode: " + ((pp != null) ? pp : "none") );
                            }

                            srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.there.stlmt" );
                            // "You can't build a settlement there."
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.now.stlmt" );
                        // "You can't build a settlement now."
                    }
                    break;

                case SOCPlayingPiece.CITY:

                    if (gameState == PLACING_CITY)
                    {
                        if (player.isPotentialCity( coord ) && (player.getNumPieces( SOCPlayingPiece.CITY ) >= 1))
                        {
                            boolean houseRuleFirstCity = game.isGameOptionSet( "N7C" ) && !game.hasBuiltCity();
                            if (houseRuleFirstCity && game.isGameOptionSet( "N7" )
                                && (game.getRoundCount() < game.getGameOptionIntValue( "N7" )))
                            {
                                // If "No 7s for first # rounds" is active, and this isn't its last round, 7s won't
                                // be rolled soon: Don't announce "Starting next turn, dice rolls of 7 may occur"
                                houseRuleFirstCity = false;
                            }

                            final SOCCity ci = new SOCCity( player, coord, null );
                            game.putPiece( ci );  // changes game state and maybe player

                            srv.gameList.takeMonitorForGame( gaName );
                            try
                            {
                                srv.messageToGameKeyed
                                    ( game, true, false, "action.built.city", plName );  // "Joe built a city."
                                srv.messageToGameWithMon
                                    ( gaName, true, new SOCPutPiece( gaName, pn, SOCPlayingPiece.CITY, coord ) );
                                if (!game.pendingMessagesOut.isEmpty())
                                    handler.sendGamePendingMessages( game, false );
                                if (houseRuleFirstCity)
                                    srv.messageToGameKeyed( game, true, false, "action.built.nextturn.7.houserule" );
                                // "Starting next turn, dice rolls of 7 may occur (house rule)."
                            }
                            finally
                            {
                                srv.gameList.releaseMonitorForGame( gaName );
                            }

                            // Check player and send new game state
                            if (!handler.checkTurn( c, game ))
                                handler.sendTurn( game, false );  // Announce new state and new current player
                            else
                                handler.sendGameState( game );
                        }
                        else
                        {
                            D.ebugPrintlnINFO( "ILLEGAL CITY: 0x" + Integer.toHexString( coord )
                                + ": player " + pn );
                            if (player.isRobot() && D.ebugOn)
                            {
                                D.ebugPrintlnINFO( " - pl.isPotentialCity: " + player.isPotentialCity( coord ) );
                                SOCPlayingPiece pp = game.getBoard().settlementAtNode( coord );
                                D.ebugPrintlnINFO( " - city/settlementAtNode: " + ((pp != null) ? pp : "none") );
                            }

                            srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.there.city" );
                            // "You can't build a city there."
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.now.city" );
                        // "You can't build a city now."
                    }
                    break;

                case SOCPlayingPiece.SHIP:

                    if ((gameState == START1B) || (gameState == START2B) || (gameState == START3B)
                        || (gameState == PLACING_SHIP)
                        || (gameState == PLACING_FREE_ROAD1) || (gameState == PLACING_FREE_ROAD2))
                    {
                        // Place it if we can; canPlaceShip checks potentials and pirate ship location
                        if (game.canPlaceShip( player, coord ) && (player.getNumPieces( SOCPlayingPiece.SHIP ) >= 1))
                        {
                            final SOCShip sh = new SOCShip( player, coord, null );
                            game.putPiece( sh );  // Changes game state and (during initial placement) sometimes player

                            srv.gameList.takeMonitorForGame( gaName );
                            try
                            {
                                srv.messageToGameKeyed
                                    ( game, true, false, "action.built.ship", plName );  // "Joe built a ship."
                                srv.messageToGameWithMon
                                    ( gaName, true, new SOCPutPiece( gaName, pn, SOCPlayingPiece.SHIP, coord ) );
                                handler.reportLongestRoadIfChanged( game, longestRoutePlayer, true );
                                if (!game.pendingMessagesOut.isEmpty())
                                    handler.sendGamePendingMessages( game, false );
                            }
                            finally
                            {
                                srv.gameList.releaseMonitorForGame( gaName );
                            }

                            // If needed, call sendTurn or send SOCRollDicePrompt
                            handler.sendTurnStateAtInitialPlacement( game, player, c, gameState );

                            GameState newState = game.getGameState();
                            if (   (newState == STARTS_WAITING_FOR_PICK_GOLD_RESOURCE)
                                || (newState == WAITING_FOR_PICK_GOLD_RESOURCE))
                            {
                                // gold hex revealed from fog (scenario SC_FOG)
                                handler.sendGameState_sendGoldPickAnnounceText( game, gaName, c, null );
                            }
                        }
                        else
                        {
                            D.ebugPrintlnINFO( "ILLEGAL SHIP: 0x" + Integer.toHexString( coord )
                                + ": player " + pn );
                            if (player.isRobot() && D.ebugOn)
                            {
                                D.ebugPrintlnINFO( " - pl.isPotentialShip: " + player.isPotentialShip( coord ) );
                                SOCPlayingPiece pp = game.getBoard().roadOrShipAtEdge( coord );
                                D.ebugPrintlnINFO( " - ship/roadAtEdge: " + ((pp != null) ? pp : "none") );
                            }

                            srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.there.ship" );
                            // "You can't build a ship there."
                            sendDenyReply = true;
                        }
                    }
                    else
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.now.ship" );
                        // "You can't build a ship now."
                    }
                    break;
                }

                if (sendDenyReply)
                {
                    if (isBuyAndPut)
                        handler.sendGameState( game );  // is probably now PLACING_*, was PLAY1 or SPECIAL_BUILDING
                    srv.messageToPlayer( c, gaName, pn, new SOCCancelBuildRequest( gaName, putPieceMessage.getPieceType() ) );
                    if (player.isRobot())
                    {
                        // Set the "force end turn soon" field
                        game.lastActionTime = 0L;
                    }
                }
            }
            else
            {   // "It's not your turn."
                srv.messageToPlayerKeyed( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED, "base.reply.not.your.turn" );
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught in handlePUTPIECE" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * @param game the SOCGame instance that this message is addressed to
     * @param movePieceMsg
     *
     * Handle the client's "move piece request" message.
     * Currently, ships are the only pieces that can be moved.
     */
    private void handleMOVEPIECE( SOCGame game, Connection c, final SOCMovePiece movePieceMsg )
    {
        final String gameName = game.getName();

        boolean denyRequest = false;
        final int fromEdge = movePieceMsg.getFromCoord(),
            toEdge = movePieceMsg.getToCoord();
        if (   (movePieceMsg.getPieceType() != SOCPlayingPiece.SHIP)
            || !handler.checkTurn( c, game ))
        {
            denyRequest = true;
        }
        else
        {
            final int pn = game.getCurrentPlayerNumber();
            SOCShip moveShip = game.canMoveShip( pn, fromEdge, toEdge );
            if (moveShip == null)
            {
                denyRequest = true;
            }
            else
            {
                final GameState gstate = game.getGameState();
                final SOCPlayer longestRoutePlayer = game.getPlayerWithLongestRoad();

                game.moveShip( moveShip, toEdge );

                srv.messageToGame( gameName, true, new SOCMovePiece
                    ( gameName, pn, SOCPlayingPiece.SHIP, fromEdge, toEdge ) );
                // client will also print "* Joe moved a ship.", no need to send a SOCGameServerText.
                handler.reportLongestRoadIfChanged( game, longestRoutePlayer, false );

                if (!game.pendingMessagesOut.isEmpty())
                    handler.sendGamePendingMessages( game, true );

                if (game.getGameState() == WAITING_FOR_PICK_GOLD_RESOURCE)
                {
                    // If ship placement reveals a gold hex in _SC_FOG,
                    // the player gets to pick a free resource.
                    handler.sendGameState( game, false, false );
                    handler.sendGameState_sendGoldPickAnnounceText( game, gameName, c, null );
                }
                else if (gstate != game.getGameState())
                {
                    // announce new state (such as PLACING_INV_ITEM in _SC_FTRI),
                    // or if state is now GameState.GAME_OVER, announce end of game
                    handler.sendGameState( game, false, false );
                }
            }
        }

        if (denyRequest)
        {
            D.ebugPrintlnINFO( "ILLEGAL MOVEPIECE: 0x" + Integer.toHexString( fromEdge ) + " -> 0x" + Integer.toHexString( toEdge )
                + ": player " + c.getData() );
            srv.messageToPlayerKeyed( c, gameName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                "reply.movepiece.cannot.now.ship" );  // "You can't move that ship now."
            srv.messageToPlayer( c, gameName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                new SOCCancelBuildRequest( gameName, SOCPlayingPiece.SHIP ) );
        }
    }

    /**
     * Handle the client's debug Free Placement putpiece request.
     * @since 1.1.12
     */
    private void handleDEBUGFREEPLACE( SOCGame game, Connection c, SOCDebugFreePlace debugFreePlaceMessage )
    {
        if (!game.isDebugFreePlacement())
            return;
        final String gaName = game.getName();
        final GameState gstate = game.getGameState();
        final SOCPlayer longestRoutePlayer = game.getPlayerWithLongestRoad();

        final int coord = debugFreePlaceMessage.getCoordinates();
        final SOCPlayer player = game.getPlayer( debugFreePlaceMessage.getPlayerNumber() );
        if (player == null)
            return;

        boolean didPut = false;
        final int pieceType = debugFreePlaceMessage.getPieceType(),
            pn = player.getPlayerNumber();

        final boolean initialDeny
            = game.isInitialPlacement() && !player.canBuildInitialPieceType( pieceType );
        boolean denyInSpecial = false;

        switch (pieceType)
        {
        case SOCPlayingPiece.ROAD:
            if (player.isPotentialRoad( coord ) && !initialDeny)
            {
                game.putPiece( new SOCRoad( player, coord, null ) );
                didPut = true;
            }
            break;

        case SOCPlayingPiece.SETTLEMENT:
            if (player.canPlaceSettlement( coord ) && !initialDeny)
            {
                game.putPiece( new SOCSettlement( player, coord, null ) );
                didPut = true;
            }
            break;

        case SOCPlayingPiece.CITY:
            if (player.isPotentialCity( coord ) && !initialDeny)
            {
                game.putPiece( new SOCCity( player, coord, null ) );
                didPut = true;
            }
            break;

        case SOCPlayingPiece.SHIP:
            if (game.canPlaceShip( player, coord ) && !initialDeny)
            {
                if (player.canPlaceShip_debugFreePlace( coord ))
                {
                    game.putPiece( new SOCShip( player, coord, null ) );
                    didPut = true;
                }
                else
                {
                    denyInSpecial = true;
                }
            }
            break;

        default:
            srv.messageToPlayer( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                "* Unknown piece type: " + pieceType );
        }

        if (didPut)
        {
            srv.messageToGame( gaName, true, new SOCPutPiece
                ( gaName, pn, pieceType, coord ) );
            handler.reportLongestRoadIfChanged( game, longestRoutePlayer, false );

            if (!(game.pendingMessagesOut.isEmpty() && player.pendingMessagesOut.isEmpty()))
                handler.sendGamePendingMessages( game, true );

            // Check for initial settlement next to gold hex, or road/ship revealed gold from fog
            final int numGoldRes = player.getNeedToPickGoldHexResources();
            final GameState newState = game.getGameState();
            if (numGoldRes > 0)
            {
                if (newState == WAITING_FOR_PICK_GOLD_RESOURCE)
                    srv.messageToGame( gaName, true, new SOCGameState( gaName, newState ));
                // state not sent for STARTS_WAITING_FOR_PICK_GOLD_RESOURCE
                srv.messageToPlayer( c, gaName, pn,
                    new SOCSimpleRequest
                        ( gaName, pn, SOCSimpleRequest.PROMPT_PICK_RESOURCES, numGoldRes ) );
            }

            if (newState.gt( ALMOST_OVER ))
            {
                // exit debug mode, announce end of game
                handler.processDebugCommand_freePlace( c, game, "0" );
                handler.sendGameState( game, false, false );
            }
            else if (newState != gstate)
            {
                handler.sendGameState( game, false, false );
            }
        }
        else
        {
            if (initialDeny)
            {
                final String pieceTypeFirst =
                    ((player.getPieces().size() % 2) == 0)
                        ? "settlement"
                        : "road";
                srv.messageToPlayer( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                    "Place a " + pieceTypeFirst + " before placing that." );
            }
            else if (denyInSpecial)
            {
                srv.messageToPlayer( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                    "Can't currently do that during Free Placement." );
            }
            else
            {
                srv.messageToPlayer( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED,
                    "Not a valid location to place that." );
            }
        }
    }


    /// Development Cards ///


    /**
     * handle "buy dev card request" message.
     *<P>
     * Before v2.0.00 this method was {@code handleBUYCARDREQUEST}.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param mes  the message
     * @since 1.0.0
     */
    private void handleBUYDEVCARDREQUEST( SOCGame game, Connection c,
        @SuppressWarnings("unused") SOCBuyDevCardRequest buyDevCardRequestMessage )
    {
        game.takeMonitor();

        try
        {
            final String gaName = game.getName();
            SOCPlayer player = game.getPlayer( c.getData() );
            final int pn = player.getPlayerNumber();
            boolean sendDenyReply = false;  // for robots' benefit

            if (handler.checkTurn( c, game ))
            {
                if (((game.getGameState() == PLAY1) || (game.getGameState() == SPECIAL_BUILDING))
                    && (game.couldBuyDevCard( pn )))
                {
                    int card = game.buyDevCard();
                    final int devCount = game.getNumDevCards();

                    // Note: If this message sequence changes, update SOCBuyDevCardRequest javadoc

                    srv.gameList.takeMonitorForGame( gaName );
                    try
                    {
                        if (game.clientVersionLowest >= SOCPlayerElements.MIN_VERSION)
                        {
                            srv.messageToGameWithMon( gaName, true, new SOCPlayerElements
                                ( gaName, pn, SOCPlayerElement.LOSE, SOCDevCard.COST ) );
                            srv.messageToGameWithMon
                                ( gaName, true, new SOCGameElements( gaName, GEType.DEV_CARD_COUNT, devCount ) );
                        }
                        else
                        {
                            srv.messageToGameWithMon( gaName, false, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.LOSE, PEType.ORE, 1 ) );
                            srv.messageToGameWithMon( gaName, false, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.LOSE, PEType.SHEEP, 1 ) );
                            srv.messageToGameWithMon( gaName, false, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.LOSE, PEType.WHEAT, 1 ) );
                            srv.messageToGameWithMon
                                ( gaName, false, new SOCDevCardCount( gaName, devCount ) );

                            if (srv.recordGameEventsIsActive())
                            {
                                srv.recordGameEvent( gaName, new SOCPlayerElements
                                    ( gaName, pn, SOCPlayerElement.LOSE, SOCDevCard.COST ) );
                                srv.recordGameEvent
                                    ( gaName, new SOCGameElements( gaName, GEType.DEV_CARD_COUNT, devCount ) );
                            }
                        }
                    }
                    finally
                    {
                        srv.gameList.releaseMonitorForGame( gaName );
                    }

                    // Let the player know, and record that event
                    {
                        if ((card == SOCDevCardConstants.KNIGHT)
                            && (c.getRemoteVersion() < SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES))
                            card = SOCDevCardConstants.KNIGHT_FOR_VERS_1_X;

                        SOCDevCardAction drawMsg = new SOCDevCardAction
                            ( gaName, pn, SOCDevCardAction.DRAW, card );
                        srv.messageToPlayer( c, null, SOCServer.PN_NON_EVENT, drawMsg );

                        if (srv.recordGameEventsIsActive())
                        {
                            if (card == SOCDevCardConstants.KNIGHT_FOR_VERS_1_X)
                                drawMsg = new SOCDevCardAction
                                    ( gaName, pn, SOCDevCardAction.DRAW, SOCDevCardConstants.KNIGHT );
                            srv.recordGameEventTo( gaName, pn, drawMsg );
                        }
                    }

                    // send as Unknown to other members of game, unless game/cards are Observable:

                    if (game.clientVersionLowest >= SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES)
                    {
                        final int ctypeToOthers =
                            (game.isGameOptionSet( SOCGameOptionSet.K_PLAY_FO )
                                || game.isGameOptionSet( SOCGameOptionSet.K_PLAY_VPO ))
                                ? card
                                : SOCDevCardConstants.UNKNOWN;

                        srv.messageToGameExcept( gaName, c, pn, new SOCDevCardAction
                            ( gaName, pn, SOCDevCardAction.DRAW, ctypeToOthers ), true );
                    }
                    else
                    {
                        srv.messageToGameForVersionsExcept
                            ( game, -1, SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES - 1,
                                c, new SOCDevCardAction
                                    ( gaName, pn, SOCDevCardAction.DRAW, SOCDevCardConstants.UNKNOWN_FOR_VERS_1_X ), true );
                        srv.messageToGameForVersionsExcept
                            ( game, SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES, Integer.MAX_VALUE,
                                c, new SOCDevCardAction
                                    ( gaName, pn, SOCDevCardAction.DRAW, SOCDevCardConstants.UNKNOWN ), true );

                        if (srv.recordGameEventsIsActive())
                            srv.recordGameEventNotTo( gaName, pn, new SOCDevCardAction
                                ( gaName, pn, SOCDevCardAction.DRAW, SOCDevCardConstants.UNKNOWN ) );
                    }

                    final int remain = game.getNumDevCards();
                    final SOCSimpleAction actmsg = new SOCSimpleAction
                        ( gaName, pn, SOCSimpleAction.DEVCARD_BOUGHT, remain, 0 );

                    if (game.clientVersionLowest >= SOCSimpleAction.VERSION_FOR_SIMPLEACTION)
                    {
                        srv.messageToGame( gaName, true, actmsg );
                    }
                    else
                    {
                        srv.recordGameEvent( gaName, actmsg );

                        srv.gameList.takeMonitorForGame( gaName );

                        try
                        {
                            srv.messageToGameForVersions
                                ( game, SOCSimpleAction.VERSION_FOR_SIMPLEACTION, Integer.MAX_VALUE, actmsg, false );

                            // Only pre-1.1.19 clients will see the game text messages. Since they're
                            // older than the i18n work: Skip text key lookups, always use english,
                            // and use SOCGameTextMsg not SOCGameServerText.

                            final String boughtTxt = MessageFormat.format
                                ( "{0} bought a development card.", player.getName() );
                            srv.messageToGameForVersions( game, -1, SOCSimpleAction.VERSION_FOR_SIMPLEACTION - 1,
                                new SOCGameTextMsg( gaName, SOCGameTextMsg.SERVERNAME, boughtTxt ), false );

                            final String remainTxt;
                            switch (remain)
                            {
                            case 0:
                                remainTxt = "There are no more Development cards.";
                                break;
                            case 1:
                                remainTxt = "There is 1 card left.";
                                break;
                            default:
                                remainTxt = MessageFormat.format( "There are {0,number} cards left.", game.getNumDevCards() );
                                // I18N OK: for old version compat
                            }
                            srv.messageToGameForVersions( game, -1, SOCSimpleAction.VERSION_FOR_SIMPLEACTION - 1,
                                new SOCGameTextMsg( gaName, SOCGameTextMsg.SERVERNAME, remainTxt ), false );
                        }
                        finally
                        {
                            srv.gameList.releaseMonitorForGame( gaName );
                        }
                    }

                    handler.sendGameState( game );
                }
                else
                {
                    // unlikely; client should know to not send request in those conditions

                    final String denyText = (game.getNumDevCards() == 0)
                        ? /*I*/"There are no more Development cards."/*18N*/
                        : /*I*/"You can't buy a development card now."/*18N*/;
                    srv.messageToPlayer( c, gaName, pn, denyText );

                    sendDenyReply = true;
                }
            }
            else
            {
                // is asking "buy" to trigger SBP if 6-player, otherwise request not allowed at this time

                sendDenyReply = true;

                if (game.maxPlayers <= 4)
                {
                    srv.messageToPlayerKeyed( c, gaName, pn, "base.reply.not.your.turn" );  // "It's not your turn."
                }
                else
                {
                    // 6-player board: Special Building Phase
                    try
                    {
                        game.askSpecialBuild( pn, true );
                        srv.messageToGame( gaName, true, new SOCPlayerElement
                            ( gaName, pn, SOCPlayerElement.SET, PEType.ASK_SPECIAL_BUILD, 1 ) );
                        sendDenyReply = false;
                    }
                    catch( NoSuchElementException e )
                    {
                        srv.messageToPlayerKeyed( c, gaName, pn, "action.build.cannot.special.PLP.common" );
                        // "House rule: Special Building phase requires 5 or 6 players."
                    }
                    catch( IllegalStateException e )
                    {
                        srv.messageToPlayer( c, gaName, pn, /*I*/"You can't ask to buy a card now."/*18N*/ );
                    }
                }
            }

            if (sendDenyReply && game.getPlayer( pn ).isRobot())
            {
                srv.messageToPlayer( c, gaName, pn, new SOCCancelBuildRequest( gaName, -2 ) );  // == SOCPossiblePiece.CARD
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "play development card request" message.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param devCardRequestMsg  the message
     * @since 1.0.0
     */
    private void handlePLAYDEVCARDREQUEST( SOCGame game, Connection c, final SOCPlayDevCardRequest devCardRequestMsg )
    {
        game.takeMonitor();

        try
        {
            final String gaName = game.getName();
            boolean denyPlayCardNow = false;  // if player can't play right now, send "You can't play a (cardtype) card now."
            String denyTextKey = null;  // if player can't play right now, for a different reason than denyPlayCardNow, send this

            if (handler.checkTurn( c, game ))
            {
                final SOCPlayer player = game.getPlayer( c.getData() );
                final int pn = player.getPlayerNumber();

                int ctype = devCardRequestMsg.getDevCard();
                if ((ctype == SOCDevCardConstants.KNIGHT_FOR_VERS_1_X)
                    && (c.getRemoteVersion() < SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES))
                    ctype = SOCDevCardConstants.KNIGHT;

                switch (ctype)
                {
                case SOCDevCardConstants.KNIGHT:
                {
                    final boolean isWarshipConvert = game.isGameOptionSet( SOCGameOptionSet.K_SC_PIRI );

                    if (game.canPlayKnight( pn ))
                    {
                        final PEType peType = (isWarshipConvert)
                            ? PEType.SCENARIO_WARSHIP_COUNT : PEType.NUMKNIGHTS;
                        final int pnWithLargestArmy;
                        {
                            SOCPlayer pl = game.getPlayerWithLargestArmy();
                            pnWithLargestArmy = (pl != null) ? pl.getPlayerNumber() : -1;
                        }

                        game.playKnight();

                        final String cardplayed = (isWarshipConvert)
                            ? "action.card.soldier.warship"  // "converted a ship to a warship."
                            : "action.card.soldier";         // "played a Soldier card."

                        srv.gameList.takeMonitorForGame( gaName );

                        srv.messageToGameKeyed( game, true, false, cardplayed, player.getName() );
                        if (game.clientVersionLowest >= SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES)
                        {
                            srv.messageToGameWithMon( gaName, true, new SOCDevCardAction
                                ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.KNIGHT ) );
                        }
                        else
                        {
                            srv.messageToGameForVersions
                                ( game, -1, SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES - 1,
                                    new SOCDevCardAction
                                        ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.KNIGHT_FOR_VERS_1_X ), false );
                            srv.messageToGameForVersions
                                ( game, SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES, Integer.MAX_VALUE,
                                    new SOCDevCardAction
                                        ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.KNIGHT ), false );

                            if (srv.recordGameEventsIsActive())
                                srv.recordGameEvent( gaName, new SOCDevCardAction
                                    ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.KNIGHT ) );
                        }

                        if (game.clientVersionLowest >= SOCPlayerElement.VERSION_FOR_CARD_ELEMENTS)
                            srv.messageToGameWithMon( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET, PEType.PLAYED_DEV_CARD_FLAG, 1 ) );
                        else
                            srv.messageToGameWithMon( gaName, true, new SOCSetPlayedDevCard( gaName, pn, true ) );

                        srv.messageToGameWithMon
                            ( gaName, true, new SOCPlayerElement( gaName, pn, SOCPlayerElement.GAIN, peType, 1 ) );

                        srv.gameList.releaseMonitorForGame( gaName );

                        if (!isWarshipConvert)
                        {
                            SOCPlayer pl = game.getPlayerWithLargestArmy();
                            int newPNwithLargestArmy = (pl != null) ? pl.getPlayerNumber() : -1;
                            if (newPNwithLargestArmy != pnWithLargestArmy)
                            {
                                SOCMessage msg;
                                if (game.clientVersionLowest >= SOCGameElements.MIN_VERSION)
                                    msg = new SOCGameElements
                                        ( gaName, GEType.LARGEST_ARMY_PLAYER, newPNwithLargestArmy );
                                else
                                    msg = new SOCLargestArmy( gaName, newPNwithLargestArmy );

                                srv.messageToGame( gaName, true, msg );
                            }

                            handler.sendGameState( game );
                        }
                    }
                    else
                    {
                        denyPlayCardNow = true;
                        // "You can't play a " + ((isWarshipConvert) ? "Warship" : "Soldier") + " card now."
                    }
                }
                break;

                case SOCDevCardConstants.ROADS:

                    if (game.canPlayRoadBuilding( pn ))
                    {
                        game.playRoadBuilding();

                        srv.gameList.takeMonitorForGame( gaName );
                        srv.messageToGameWithMon( gaName, true, new SOCDevCardAction
                            ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.ROADS ) );
                        if (game.clientVersionLowest >= SOCPlayerElement.VERSION_FOR_CARD_ELEMENTS)
                            srv.messageToGameWithMon( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET, PEType.PLAYED_DEV_CARD_FLAG, 1 ) );
                        else
                            srv.messageToGameWithMon( gaName, true, new SOCSetPlayedDevCard( gaName, pn, true ) );
                        srv.messageToGameKeyed( game, true, false, "action.card.roadbuilding", player.getName() );
                        // "played a Road Building card."
                        srv.gameList.releaseMonitorForGame( gaName );

                        handler.sendGameState( game );
                        if (game.getGameState() == PLACING_FREE_ROAD1)
                            srv.messageToPlayerKeyed
                                ( c, gaName, pn, (game.hasSeaBoard) ? "action.card.road.place.2s" : "action.card.road.place.2r" );
                            // "You may place 2 roads/ships." or "You may place 2 roads."
                        else
                            srv.messageToPlayerKeyed
                                ( c, gaName, pn, (game.hasSeaBoard) ? "action.card.road.place.1s" : "action.card.road.place.1r" );
                        // "You may place your 1 remaining road or ship." or "... place your 1 remaining road."
                    }
                    else
                    {
                        denyPlayCardNow = true;  // "You can't play a Road Building card now."
                    }

                    break;

                case SOCDevCardConstants.DISC:

                    if (game.canPlayDiscovery( pn ))
                    {
                        game.playDiscovery();

                        srv.gameList.takeMonitorForGame( gaName );
                        srv.messageToGameWithMon( gaName, true, new SOCDevCardAction
                            ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.DISC ) );
                        if (game.clientVersionLowest >= SOCPlayerElement.VERSION_FOR_CARD_ELEMENTS)
                            srv.messageToGameWithMon( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET, PEType.PLAYED_DEV_CARD_FLAG, 1 ) );
                        else
                            srv.messageToGameWithMon( gaName, true, new SOCSetPlayedDevCard( gaName, pn, true ) );
                        srv.messageToGameKeyed( game, true, false, "action.card.discoveryplenty", player.getName() );
                        // "played a Year of Plenty card."
                        srv.gameList.releaseMonitorForGame( gaName );

                        handler.sendGameState( game );
                    }
                    else
                    {
                        denyPlayCardNow = true;  // "You can't play a Year of Plenty card now."
                    }

                    break;

                case SOCDevCardConstants.MONO:

                    if (game.canPlayMonopoly( pn ))
                    {
                        game.playMonopoly();

                        srv.gameList.takeMonitorForGame( gaName );
                        srv.messageToGameWithMon( gaName, true, new SOCDevCardAction
                            ( gaName, pn, SOCDevCardAction.PLAY, SOCDevCardConstants.MONO ) );
                        if (game.clientVersionLowest >= SOCPlayerElement.VERSION_FOR_CARD_ELEMENTS)
                            srv.messageToGameWithMon( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET, PEType.PLAYED_DEV_CARD_FLAG, 1 ) );
                        else
                            srv.messageToGameWithMon( gaName, true, new SOCSetPlayedDevCard( gaName, pn, true ) );
                        srv.messageToGameKeyed( game, true, false, "action.card.mono", player.getName() );  // "played a Monopoly card."
                        srv.gameList.releaseMonitorForGame( gaName );

                        handler.sendGameState( game );
                    }
                    else
                    {
                        denyPlayCardNow = true;  // "You can't play a Monopoly card now."
                    }

                    break;

                // VP cards are secretly played when bought.
                // (case SOCDevCardConstants.CAP, MARKET, UNIV, TEMP, CHAPEL):
                // If player clicks "Play Card" the message is handled at the
                // client, in SOCHandPanel.actionPerformed case CARD.
                //  "You secretly played this VP card when you bought it."
                //  break;

                default:
                    denyTextKey = "reply.playdevcard.type.unknown";  // "That card type is unknown."
                    D.ebugPrintlnINFO( "* srv handlePLAYDEVCARDREQUEST: asked to play unhandled type " + devCardRequestMsg.getDevCard() );
                    // debug prints dev card type from client, not ctype,
                    // in case ctype was changed here from message value.

                }
            }
            else
            {
                denyTextKey = "base.reply.not.your.turn";  // "It's not your turn."
            }

            if (denyPlayCardNow || (denyTextKey != null))
            {
                final SOCClientData scd = (SOCClientData) c.getAppData();
                if ((scd == null) || !scd.isRobot)
                {
                    if (denyTextKey != null)
                        srv.messageToPlayerKeyed
                            ( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED, denyTextKey );
                    else
                        srv.messageToPlayerKeyedSpecial
                            ( c, game, SOCServer.PN_REPLY_TO_UNDETERMINED, "reply.playdevcard.cannot.now", devCardRequestMsg.getDevCard() );
                }
                else
                {
                    srv.messageToPlayer
                        ( c, null, SOCServer.PN_REPLY_TO_UNDETERMINED,
                            new SOCDevCardAction( gaName, -1, SOCDevCardAction.CANNOT_PLAY, devCardRequestMsg.getDevCard() ) );
                }
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "discovery pick" (while playing Discovery/Year of Plenty card) / Gold Hex resource pick message.
     *
     * @param game the SOCGame instance that this message is addressed to
     * @param c  the connection that sent the message
     * @param mes  the message
     * @since 1.0.0
     */
    private void handlePICKRESOURCES( SOCGame game, Connection c, final SOCPickResources mes )
    {
        final String gameName = game.getName();
        final SOCResourceSet rsrcs = mes.getResources();

        game.takeMonitor();

        final SOCPlayer player = game.getPlayer( c.getData() );
        final int seatNumber;
        if (player != null)
            seatNumber = player.getPlayerNumber();
        else
            seatNumber = -1;  // c's client no longer in the game

        try
        {
            if (player == null)
            {
                // The catch block will print this out semi-nicely
                throw new IllegalArgumentException( "player not found in game" );
            }

            final GameState startState = game.getGameState();
            if (startState == WAITING_FOR_DISCOVERY)
            {
                // Message is Discovery/Year of Plenty picks
                if (handler.checkTurn( c, game ))
                {
                    if (game.canDoDiscoveryAction( rsrcs ))
                    {
                        game.doDiscoveryAction( rsrcs );

                        final SOCPickResources picked = new SOCPickResources
                            (gameName, rsrcs, seatNumber, SOCPickResources.REASON_DISCOVERY);
                        if (game.clientVersionLowest >= SOCPickResources.VERSION_FOR_SERVER_ANNOUNCE)
                        {
                            srv.messageToGame(gameName, true, picked);
                        }
                        else
                        {
                            srv.recordGameEvent(gameName, picked);

                            srv.messageToGameForVersions
                                (game, SOCPickResources.VERSION_FOR_SERVER_ANNOUNCE, Integer.MAX_VALUE, picked, true);

                            handler.reportRsrcGainLossForVersions
                                (game, rsrcs, false, true, seatNumber, -1, null, SOCPickResources.VERSION_FOR_SERVER_ANNOUNCE - 1);
                            srv.messageToGameForVersionsKeyed
                                (game, 0, SOCPickResources.VERSION_FOR_SERVER_ANNOUNCE - 1, true, true,
                                 "action.card.discov.received", player.getName(), rsrcs);
                                     // "{0} received {1,rsrcs} from the bank."
                        }
                        handler.sendGameState( game );
                    }
                    else
                    {
                        srv.messageToPlayerKeyed( c, gameName, seatNumber, "action.card.discov.notlegal" );
                        // "That is not a legal Year of Plenty pick."
                    }
                }
                else
                {
                    srv.messageToPlayerKeyed( c, gameName, seatNumber, "base.reply.not.your.turn" );  // "It's not your turn."
                }
            }
            else
            {
                // Message is Gold Hex picks
                if (game.canPickGoldHexResources( seatNumber, rsrcs ))
                {
//                    final boolean fromInitPlace = game.isInitialPlacement();
                    final boolean fromPirateFleet = game.isPickResourceIncludingPirateFleet( seatNumber );

                    GameState nextState = game.pickGoldHexResources( seatNumber, rsrcs );
                    // This assumes that pickGoldHexResources might change the game state!
                    GameState lastState = game.getGameState();

                    /**
                     * tell everyone what the player gained
                     */
                    handler.reportRsrcGainGold( game, player, seatNumber, rsrcs, false, !fromPirateFleet );

                    /**
                     * send the new state, or end turn if was marked earlier as forced
                     * -- for gold during initial placement, current player might also change.
                     */
                    if (   (startState != PLAY1)
                        || (lastState == WAITING_FOR_DISCARDS)
                        || !game.isForcingEndTurn())
                    {
                        if (!game.isInitialPlacement())
                        {
                            if (lastState == SENDING_DICE_RESULT_RESOURCES) // != WAITING_FOR_PICK_GOLD_RESOURCE??
                                game.setGameState( nextState );
                            handler.sendGameState( game );

                            if (lastState == WAITING_FOR_DISCARDS)
                            {
                                // happens only in scenario _SC_PIRI, when 7 is rolled, player wins against pirate fleet
                                // and has picked their won resource, and then someone must discard
                                handler.sendGameState_sendDiscardRequests( game, gameName );
                            }
                        }
                        else
                        {
                            // send state, and current player if changed

                            switch (lastState)
                            {
                            case START1B:
                            case START2B:
                            case START3B:
                                // pl not changed: previously placed settlement, now placing road or ship
                                handler.sendGameState( game );
                                break;

                            case START1A:
                            case START2A:
                            case START3A:
                            case ROLL_OR_CARD:
                                // Current player probably changed, announce new player if so
                                // with sendTurn and/or SOCRollDicePrompt
                                handler.sendTurnStateAtInitialPlacement( game, player, c, nextState );
                                break;
                            }
                        }
                    }
                    else
                    {
                        // force-end game turn
                        handler.endGameTurn( game, player, true );  // locking: already did ga.takeMonitor()
                    }
                }
                else
                {
                    // Can't pick because resource count or game state was wrong

                    final int npick = player.getNeedToPickGoldHexResources();
                    if (npick != rsrcs.getTotal())
                    {
                        srv.messageToPlayerKeyed
                            ( c, gameName, seatNumber, "reply.pick.gold.cannot.that_many" );  // "You can't pick that many resources."
                        if ((npick > 0) && (startState.lt( GAME_OVER )))
                            srv.messageToPlayer( c, gameName, seatNumber, new SOCSimpleRequest
                                ( gameName, seatNumber, SOCSimpleRequest.PROMPT_PICK_RESOURCES, npick ) );
                        else
                            srv.messageToPlayer( c, gameName, seatNumber, new SOCPlayerElement
                                ( gameName, seatNumber, SOCPlayerElement.SET, PEType.NUM_PICK_GOLD_HEX_RESOURCES, 0 ) );
                    }
                    else
                    {
                        // Probably a game logic bug:
                        // Some recent action/event gave the player free resource(s)
                        // but didn't follow up with a state where they're expected to claim them.
                        srv.messageToGame( gameName, true,
                            "Recovering from buggy state: " + player.getName()
                                + " won free resources but game state didn't allow them to be picked; giving them anyway." );
                        // i18n OK: won't appear in normal gameplay
                        player.getResources().add( rsrcs );
                        player.setNeedToPickGoldHexResources( 0 );
                        handler.reportRsrcGainGold( game, player, seatNumber, rsrcs, true, false );
                    }
                }
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            game.releaseMonitor();
        }
    }

    /**
     * handle "pick resource type" (monopoly) message.
     *<P>
     * Before v2.0.00 this method was {@code handleMONOPOLYPICK}.
     *
     * @param c     the connection that sent the message
     * @param mes   the message
     * @since 1.0.0
     */
    private void handlePICKRESOURCETYPE( SOCGame ga, Connection c, final SOCPickResourceType mes )
    {
        ga.takeMonitor();

        try
        {
            final String gaName = ga.getName();
            if (handler.checkTurn( c, ga ))
            {
                if (ga.canDoMonopolyAction())
                {
                    final int rsrc = mes.getResourceType();

                    final int[] monoPicks = ga.doMonopolyAction( rsrc );

                    final boolean[] isVictim = new boolean[ga.maxPlayers];
                    final int cpn = ga.getCurrentPlayerNumber();
                    final String monoPlayerName = c.getData();
                    int monoTotal = 0;
                    for (int pn = 0; pn < ga.maxPlayers; ++pn)
                    {
                        final int n = monoPicks[pn];
                        if (n > 0)
                        {
                            monoTotal += n;
                            isVictim[pn] = true;
                        }
                    }

                    srv.gameList.takeMonitorForGame( gaName );

                    /**
                     * Send each affected player's resource counts for the monopolized resource;
                     * set isNews flag for each victim player's count.
                     * Sending rsrc number works because SOCPlayerElement.CLAY == SOCResourceConstants.CLAY.
                     */
                    for (int pn = 0; pn < ga.maxPlayers; ++pn)
                        if (isVictim[pn])
                            srv.messageToGameWithMon
                                (gaName, true, new SOCPlayerElement
                                    (gaName, pn, SOCPlayerElement.SET,
                                     rsrc, ga.getPlayer(pn).getResources().getAmount(rsrc), true));
                    srv.messageToGameWithMon
                        (gaName, true, new SOCPlayerElement
                            (gaName, cpn, SOCPlayerElement.GAIN,
                             rsrc, monoTotal, false));

                    final SOCSimpleAction actMsg = new SOCSimpleAction
                        ( gaName, cpn,
                            SOCSimpleAction.RSRC_TYPE_MONOPOLIZED, monoTotal, rsrc );
                    // Client will print "You monopolized 5 sheep." or "Joe monopolized 5 Sheep."

                    if (ga.clientVersionLowest >= SOCStringManager.VERSION_FOR_I18N)
                    {
                        srv.messageToGameWithMon( gaName, true, actMsg );
                    }
                    else
                    {
                        srv.recordGameEvent( gaName, actMsg );

                        srv.messageToGameForVersions
                            ( ga, SOCStringManager.VERSION_FOR_I18N, Integer.MAX_VALUE, actMsg, false );

                        // Only pre-2.0.00 clients need this game text message. Since they're older than
                        // the i18n work: Always use english, send SOCGameTextMsg not SOCGameServerText.

                        final String monoTxt = monoPlayerName + " monopolized " +
                            SOCStringManager.getFallbackServerManagerForClient().getSOCResourceCount( rsrc, monoTotal );
                        // "Joe monopolized 5 Sheep."
                        // If acting player's client is old, they'll see that instead of "You monopolized 5 sheep";
                        // that cosmetic change is OK.

                        srv.messageToGameForVersions( ga, -1, SOCStringManager.VERSION_FOR_I18N - 1,
                            new SOCGameTextMsg( gaName, SOCGameTextMsg.SERVERNAME, monoTxt ), false );
                    }

                    srv.gameList.releaseMonitorForGame( gaName );

                    /**
                     * now that monitor is released, notify the
                     * victim(s) of resource amounts taken.
                     * Skip robot victims, they ignore text messages.
                     */
                    for (int pn = 0; pn < ga.maxPlayers; ++pn)
                    {
                        if (!isVictim[pn])
                            continue;

                        int picked = monoPicks[pn];
                        SOCPlayer victim = ga.getPlayer( pn );
                        String viName = victim.getName();
                        Connection viCon = srv.getConnection( viName );
                        if ((viCon != null) && !victim.isRobot())
                            srv.messageToPlayerKeyedSpecial
                                ( viCon, ga, pn,
                                    ((picked == 1) ? "action.mono.took.your.1" : "action.mono.took.your.n"),
                                    monoPlayerName, picked, rsrc );
                        // "Joe's Monopoly took your 3 sheep."
                    }

                    handler.sendGameState( ga );
                }
                else
                {
                    srv.messageToPlayerKeyedSpecial
                        ( c, ga, SOCServer.PN_REPLY_TO_UNDETERMINED, "reply.playdevcard.cannot.now", SOCDevCardConstants.MONO );
                    // "You can't play a Monopoly card now."  Before v2.0.00, was "You can't do a Monopoly pick now."
                }
            }
            else
            {
                srv.messageToPlayerKeyed
                    ( c, gaName, SOCServer.PN_REPLY_TO_UNDETERMINED, "base.reply.not.your.turn" );  // "It's not your turn."
            }
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            ga.releaseMonitor();
        }
    }


    /// Inventory Items and Special Items ///


    /**
     * Special inventory item action (play request) from a player.
     * Ignored unless {@link SOCInventoryItemAction#action mes.action} == {@link SOCInventoryItemAction#PLAY PLAY}.
     * Calls {@link SOCGame#canPlayInventoryItem(int, int)}, {@link SOCGame#playInventoryItem(int)}.
     * If game state changes here, calls {@link SOCGameHandler#sendGameState(SOCGame)} just before returning.
     *
     * @param ga  game with {@code c} as a client player
     * @param c  the connection sending the message
     * @param mes  the message
     */
    private void handleINVENTORYITEMACTION( SOCGame ga, Connection c, final SOCInventoryItemAction mes )
    {
        if (mes.action != SOCInventoryItemAction.PLAY)
            return;

        final String gaName = ga.getName();
        SOCPlayer clientPl = ga.getPlayer( c.getData() );
        if (clientPl == null)
            return;

        final int pn = clientPl.getPlayerNumber();

        final int replyCannot = ga.canPlayInventoryItem( pn, mes.itemType );
        if (replyCannot != 0)
        {
            srv.messageToPlayer( c, gaName, pn,
                new SOCInventoryItemAction
                    ( gaName, -1, SOCInventoryItemAction.CANNOT_PLAY, mes.itemType, replyCannot ) );
            return;
        }

        GameState oldGameState = ga.getGameState();

        final SOCInventoryItem item = ga.playInventoryItem( mes.itemType );  // <--- Play the item ---

        if (item == null)
        {
            // Wasn't able to play.  Assume canPlay was recently called and returned OK; the most
            // volatile of its conditions is player's inventory, so assume that's what changed.
            srv.messageToPlayer( c, gaName, pn,
                new SOCInventoryItemAction
                    ( gaName, -1, SOCInventoryItemAction.CANNOT_PLAY, mes.itemType, 1 ) );  // 1 == item not in inventory
            return;
        }

        // Item played.  Announce play and removal (or keep) from player's inventory.
        // Announce game state if changed.
        srv.messageToGame( gaName, true,
            new SOCInventoryItemAction( gaName, pn, SOCInventoryItemAction.PLAYED, item.itype,
                    item.isKept(), item.isVPItem(), item.canCancelPlay ));

        if (ga.getGameState() != oldGameState)
            handler.sendGameState( ga );
    }

    /**
     * Handle Special Item requests from a player.
     * Calls {@link SOCSpecialItem#playerPickItem(String, SOCGame, SOCPlayer, int, int)}
     * or {@link SOCSpecialItem#playerSetItem(String, SOCGame, SOCPlayer, int, int, boolean)}
     * which provide scenario-specific responses or decline the request.
     *<P>
     * As with building a settlement or road, cost paid isn't reported as a text message:
     * If that's important to the client, they already have {@link SOCSpecialItem#getCost()}
     * and can print something when they receive the server's SOCSetSpecialItem message.
     *
     * @param c  the connection that sent the message
     * @param mes  the message
     */
    private void handleSETSPECIALITEM( SOCGame ga, Connection c, final SOCSetSpecialItem mes )
    {
        final String gaName = ga.getName();
        final SOCPlayer pl = ga.getPlayer( c.getData() );
        final String typeKey = mes.typeKey;
        final int op = mes.op, gi = mes.gameItemIndex, pi = mes.playerItemIndex;
        final int pn = (pl != null) ? pl.getPlayerNumber() : -1;  // don't trust mes.playerNumber
        boolean sendDenyReply = false;

        try
        {
            SOCSpecialItem itm = null;
            final boolean paidCost;  // if true, itm's cost was paid by player to PICK or SET or CLEAR

            ga.takeMonitor();
            if ((pl == null) || (op < SOCSetSpecialItem.OP_SET) || (op > SOCSetSpecialItem.OP_PICK))
            {
                sendDenyReply = true;
            }
            else
            {
                GameState prevState = ga.getGameState();

                if (op == SOCSetSpecialItem.OP_PICK)
                {
                    int replyPickOp = SOCSetSpecialItem.OP_PICK;  // may change to OP_SET_PICK or OP_CLEAR_PICK
                    int pickPN = pn, pickCoord = -1, pickLevel = 0;  // field values to send in reply/announcement
                    boolean isStartingPick = false;  // if true, player paid starting-pick cost
                    String pickSV = null;  // sv field value to send

                    // When game index and player index are both given,
                    // compare items before and after PICK in case they change
                    final SOCSpecialItem gBefore, pBefore;
                    if ((gi != -1) && (pi != -1))
                    {
                        gBefore = ga.getSpecialItem( typeKey, gi );
                        pBefore = pl.getSpecialItem( typeKey, pi );
                    }
                    else
                    {
                        gBefore = null;
                        pBefore = null;
                    }

                    // Before pick, get item as per playerPickItem javadoc for cost, coord, level,
                    // in case it's cleared by the pick. If not cleared, will get it again afterwards.
                    itm = ga.getSpecialItem( typeKey, gi, pi, pn );
                    if (itm != null)
                    {
                        pickCoord = itm.getCoordinates();
                        pickLevel = itm.getLevel();
                        pickSV = itm.getStringValue();
                        isStartingPick = (0 == pickLevel) && (null == itm.getPlayer());
                    }

                    // perform the PICK in game; if not allowed, throws IllegalStateException
                    paidCost = SOCSpecialItem.playerPickItem( typeKey, ga, pl, gi, pi );

                    // if cost paid, send resource-loss first
                    if (paidCost && (itm != null))
                        handler.reportRsrcGainLoss( ga, itm.getCost(), true, false, pn, -1, null );

                    // Next, send SET/CLEAR before sending PICK announcement

                    // For now, this send logic handles everything we need it to do.
                    // Depending on usage of PICK messages in future scenarios,
                    // we might need more info returned from playerPickItem then.

                    if ((gi == -1) || (pi == -1))
                    {
                        // request didn't specify both gi and pi: only 1 SET/CLEAR message to send.
                        // gi, pi, pn fields will all be same for SET/CLEAR & PICK:
                        // combine OP_SET or OP_CLEAR into upcoming OP_PICK reply

                        final SOCSpecialItem itmAfter = ga.getSpecialItem( typeKey, gi, pi, pn );
                        if (itmAfter != null)
                        {
                            replyPickOp = SOCSetSpecialItem.OP_SET_PICK;
                            pickPN = ((itmAfter.getPlayer() != null) ? itmAfter.getPlayer().getPlayerNumber() : -1);
                            pickCoord = itmAfter.getCoordinates();
                            pickLevel = itmAfter.getLevel();
                            pickSV = itmAfter.getStringValue();
                        }
                        else
                        {
                            replyPickOp = SOCSetSpecialItem.OP_CLEAR_PICK;
                        }
                    }
                    else
                    {
                        // request specified both gi and pi: might need to send 1 SET/CLEAR message if shared,
                        // or 2 messages if not the same object for both

                        final SOCSpecialItem gAfter, pAfter;
                        gAfter = ga.getSpecialItem( typeKey, gi );
                        pAfter = pl.getSpecialItem( typeKey, pi );

                        if (gAfter == pAfter)
                        {
                            // same object, and gi, pi, pn fields will all be same for SET/CLEAR & PICK:
                            // combine OP_SET or OP_CLEAR into upcoming OP_PICK reply
                            if (gAfter != null)
                            {
                                replyPickOp = SOCSetSpecialItem.OP_SET_PICK;
                                pickCoord = gAfter.getCoordinates();
                                pickLevel = gAfter.getLevel();
                                pickSV = gAfter.getStringValue();
                            }
                            else
                            {
                                replyPickOp = SOCSetSpecialItem.OP_CLEAR_PICK;
                            }
                        }
                        else
                        {
                            // gi and pi don't share the same object; might need to send out 2 replies if both objs changed
                            boolean setPickFieldsFromGAfter = false;

                            if (gAfter == null)
                            {
                                if (gBefore != null)
                                    srv.messageToGame( gaName, true, new SOCSetSpecialItem
                                        ( gaName, SOCSetSpecialItem.OP_CLEAR, typeKey, gi, -1, -1 ) );
                            }
                            else
                            {
                                srv.messageToGame( gaName, true, new SOCSetSpecialItem
                                    ( ga, SOCSetSpecialItem.OP_SET, typeKey, gi, -1, gAfter ) );

                                pickCoord = gAfter.getCoordinates();
                                pickLevel = gAfter.getLevel();
                                pickSV = gAfter.getStringValue();
                                setPickFieldsFromGAfter = true;
                            }

                            if (pAfter == null)
                            {
                                if (pBefore != null)
                                    srv.messageToGame( gaName, true, new SOCSetSpecialItem
                                        ( gaName, SOCSetSpecialItem.OP_CLEAR, typeKey, -1, pi, pn ) );
                            }
                            else
                            {
                                srv.messageToGame( gaName, true, new SOCSetSpecialItem
                                    ( ga, SOCSetSpecialItem.OP_SET, typeKey, -1, pi, pAfter ) );
                                if (!setPickFieldsFromGAfter)
                                {
                                    pickCoord = pAfter.getCoordinates();
                                    pickLevel = pAfter.getLevel();
                                    pickSV = pAfter.getStringValue();
                                }
                            }
                        }
                    }

                    srv.messageToGame( gaName, true, new SOCSetSpecialItem
                        ( gaName, replyPickOp, typeKey, gi, pi, pickPN, pickCoord, pickLevel, pickSV ) );

                    if (isStartingPick)
                    {
                        final int startCostPType = itm.getStartingCostPiecetype();
                        if (startCostPType != -1)
                            // report player's new total piecetype count, to avoid confusion if client decremented it
                            srv.messageToGame( gaName, true, new SOCPlayerElement
                                ( gaName, pn, SOCPlayerElement.SET,
                                    SOCPlayerElement.elementTypeForPieceType( startCostPType ),
                                    pl.getNumPieces( startCostPType ) ) );
                    }
                }
                else
                {
                    // OP_SET or OP_CLEAR

                    if (op == SOCSetSpecialItem.OP_CLEAR)
                        // get item before CLEAR
                        itm = ga.getSpecialItem( typeKey, gi, pi, pn );

                    paidCost = SOCSpecialItem.playerSetItem
                        ( typeKey, ga, pl, gi, pi, (op == SOCSetSpecialItem.OP_SET) );

                    // if cost paid, send resource-loss first
                    if (paidCost && (itm != null))
                        handler.reportRsrcGainLoss( ga, itm.getCost(), true, false, pn, -1, null );

                    // get item after SET, in case it's changed
                    if (op != SOCSetSpecialItem.OP_CLEAR)
                        itm = ga.getSpecialItem( typeKey, gi, pi, pn );

                    if ((op == SOCSetSpecialItem.OP_CLEAR) || (itm == null))
                        srv.messageToGame( gaName, true, new SOCSetSpecialItem
                            ( gaName, SOCSetSpecialItem.OP_CLEAR, typeKey, gi, pi, pn ) );
                    else
                        srv.messageToGame( gaName, true,
                            new SOCSetSpecialItem( ga, op, typeKey, gi, pi, itm ) );
                }

                // check game state, check for winner
                if (ga.getGameState() != prevState)
                    handler.sendGameState( ga );  // might be GAME_OVER, if player won
            }
        }
        catch( IllegalStateException e )
        {
            sendDenyReply = true;
        }
        catch( Exception e )
        {
            D.ebugPrintStackTrace( e, "Exception caught" );
        }
        finally
        {
            ga.releaseMonitor();
        }

        if (sendDenyReply)
            srv.messageToPlayer
                ( c, gaName, (pn >= 0) ? pn : SOCServer.PN_REPLY_TO_UNDETERMINED,
                    new SOCSetSpecialItem
                        ( gaName, SOCSetSpecialItem.OP_DECLINE, typeKey, gi, pi, mes.playerNumber ) );
    }
}
