/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2019 Colin Werner
 * Extracted in 2019 from SOCPlayergetClient().java, so:
 * Portions of this file Copyright (C) 2007-2023 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2012-2013 Paul Bilnoski <paul@bilnoski.net>
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *<p/>
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.client;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Locale;

import soc.baseclient.MessageHandler;
// import soc.baseclient.SOCDisplaylessPlayerClient;
import soc.disableDebug.D;
import soc.game.SOCBoardLarge;
import soc.game.SOCDevCardConstants;
import soc.game.SOCFortress;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCScenario;
import soc.game.SOCSettlement;
import soc.game.SOCTradeOffer;
import soc.game.SOCVillage;

import soc.message.*;
import soc.message.SOCGameElements.GEType;
import soc.message.SOCPlayerElement.PEType;
import soc.util.SOCFeatureSet;
import soc.util.SOCGameList;
import soc.util.SOCStringManager;
import soc.util.Version;

/**
 * Class for processing incoming messages (treating).
 * {@link soc.baseclient.ServerConnection}'s reader thread calls
 * {@link #handle(SOCMessage)} to dispatch messages to their
 * handler methods such as {@link #handleBANKTRADE(SOCBankTrade)}.
 *<P>
 * Before v2.0.00, most of these fields and methods were part of the main {@link SOCPlayerClient} class.
 *
 * @author paulbilnoski and Lee Passey
 * @since 2.8.00
 */
// @ Suppress Warnings("ConstantConditions")
public class PlayerMessageHandler extends MessageHandler
{
    /**
     * Get this PlayerMessageHandler's client.
     * @since 2.5.00
     */
    protected SOCPlayerClient getClient()
    {
        return (SOCPlayerClient) client;
    }
    
    /** Constructor **/
    
    /**
     * Create a PlayerMessageHandler to handle incoming messages.
     */
    public PlayerMessageHandler( SOCPlayerClient client )
    {
        super( client );
    }

    /**
     * Initial setup for {@code client} and {@code messageSender} fields.
     * To allow subclassing, that isn't done in the constructor: The client constructor
     * would want a PlayerMessageHandler, and the PlayerMessageHandler constructor would want a getClient().
     *
     * @param cli  Client for this PlayerMessageHandler; its {@link SOCPlayerClient#getGameMessageSender()} must not be null
     * @throws IllegalArgumentException if {@code cli} or its {@code getGameMessageSender()} is null
     * @since 2.5.00
     */
//    public void init(final SOCPlayerClient cli)
//        throws IllegalArgumentException
//    {
//        if (cli == null)
//            throw new IllegalArgumentException("client is null");
//
//        this.client = cli;    // My client must be a visual client or sub-class
//        gms = cli.getGameMessageSender();
//        if (gms == null)
//            throw new IllegalArgumentException("client GameMessageSender is null");
//    }
    
    /* Start message handler */
    
    /**
     * Handle the incoming messages.
     * Messages of unknown type are passed to the base class
     * ({@code mes} will be null from {@link SOCMessage#toMsg(String)}).
      *<P>
     * Before v2.0.00 this method was {@code SOCPlayergetClient().treat(..)}.
     *
     * @param mes    the message
     * @param didDebugPrintAlready  Already printed a debug message, don't do it again
     */
    @Override
    public void handle( SOCMessage mes, boolean didDebugPrintAlready  )
    {
        if (mes == null)
            return;  // Parsing error
        boolean traffic = getClient().debugTraffic;
        if (  !didDebugPrintAlready
            && (getClient().debugTraffic || D.ebugIsEnabled()))
        {
            if (!(mes instanceof SOCServerPing ))
                soc.debug.D.ebugPrintlnINFO( "IN - " + getClient().getNickname() + " - " + mes.toString() );
            didDebugPrintAlready = true;
        }
        
        try
        {
            final String gaName;
            final SOCGame game;
            if (mes instanceof SOCMessageForGame)
            {
                gaName = ((SOCMessageForGame) mes).getGameName();
                game = (gaName != null) ? getClient().getGame( gaName ) : null;
                // Allows null gaName, for the few message types (like SOCScenarioInfo) which
                // for convenience use something like SOCTemplateMs which extends SOCMessageForGame
                // but aren't actually game-specific messages.
            }
            else
            {
                gaName = null;
                game = null;
            }
            
            switch (mes.getType())
            {
                /*
                    Messages that the super class handles, but for which this class
                    has overridden the implementation. We rely on the super classes
                    switch cases to call these overridden methods.
                 */
            /**
             * server's version message
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.VERSION:
//                handleVERSION( /* isPractice,*/ (SOCVersion) mes );
//                break;
            
            /**
             * status message
             */
//            case SOCMessage.STATUSMESSAGE:
//                handleSTATUSMESSAGE( (SOCStatusMessage) mes );
//                break;
            
            /**
             * echo the server ping, to ensure we're still connected.
             * (ignored before version 1.1.08)
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.SERVERPING:
//                handleSERVERPING( (SOCServerPing) mes );
//                break;
            
            /**
             * join channel authorization
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.JOINCHANNELAUTH:
//                handleJOINCHANNELAUTH((SOCJoinChannelAuth) mes);
//                break;
            
            
             /**
             * someone left a game
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.LEAVEGAME:
//                handleLEAVEGAME((SOCLeaveGame) mes);
//                break;
            
             /**
             * game stats
              * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.GAMESTATS:
//                handleGAMESTATS( (SOCGameStats) mes );
//                break;
            
             /**
             * someone is sitting down
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.SITDOWN:
//                handleSITDOWN((SOCSitDown) mes);
//                break;
            
            /**
             * receive a board layout
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.BOARDLAYOUT:
//                handleBOARDLAYOUT((SOCBoardLayout) mes);
//                break;
            
            /**
             * receive a board layout (new format, as of 20091104 (v 1.1.08))
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.BOARDLAYOUT2:
//                handleBOARDLAYOUT2((SOCBoardLayout2) mes);
//                break;
            
            /**
             * message that the game is starting
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.STARTGAME:
//                handleSTARTGAME((SOCStartGame) mes);
//                break;
            
            /**
             * update the state of the game
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.GAMESTATE:
//                handleGAMESTATE((SOCGameState) mes);
//                break;
            
            /**
             * set the current turn
             * handled in the base class, not overridden
             */
//            case SOCMessage.SETTURN:
//                handleGAMEELEMENT(ga, GEType.CURRENT_PLAYER, ((SOCSetTurn) mes).getPlayerNumber());
//                break;
            
            /**
             * set who the first player is
             * handled in the base class, not overridden
             */
//            case SOCMessage.FIRSTPLAYER:
//                handleGAMEELEMENT(ga, GEType.FIRST_PLAYER, ((SOCFirstPlayer) mes).getPlayerNumber());
//                break;
            
            /**
             * update whose turn it is
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.TURN:
//                handleTURN((SOCTurn) mes);
//                break;
            
            /**
             * receive player information
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.PLAYERELEMENT:
//                handlePLAYERELEMENT((SOCPlayerElement) mes);
//                break;
            
            /**
             * receive player information.
             * Added 2017-12-10 for v2.0.00.
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.PLAYERELEMENTS:
//                handlePLAYERELEMENTS((SOCPlayerElements) mes);
//                break;
            
            /**
             * update game element information.
             * Added 2017-12-24 for v2.0.00.
             * Handled in the base class, not overridden
             */
//            case SOCMessage.GAMEELEMENTS:
//                handleGAMEELEMENTS((SOCGameElements) mes);
//                break;
            
            /**
             * receive resource count
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.RESOURCECOUNT:
//                handleRESOURCECOUNT((SOCResourceCount) mes);
//                break;
            
            /**
             * receive player's last settlement location.
             * Added 2017-12-23 for v2.0.00.
             * Handled in the base class, not overridden
             */
//            case SOCMessage.LASTSETTLEMENT:
//                handleLASTSETTLEMENT((SOCLastSettlement) mes, getClient().getGame( ((SOCLastSettlement) mes).getGameName()));
//                break;
            
            /**
             * the latest dice result
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.DICERESULT:
//                handleDICERESULT( (SOCDiceResult) mes );
//                break;
            
             /**
             * the current player has cancelled an initial settlement,
             * or has tried to place a piece illegally.
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.CANCELBUILDREQUEST:
//                handleCANCELBUILDREQUEST((SOCCancelBuildRequest) mes);
//                break;
            
            /**
             * the robber or pirate moved
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.MOVEROBBER:
//                handleMOVEROBBER((SOCMoveRobber) mes);
//                break;
            
             /**
             * a player has cleared her offer
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.CLEAROFFER:
//                handleCLEAROFFER((SOCClearOffer) mes);
//                break;
            
            /**
             * a dev card action, either draw, play, or add to hand
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.DEVCARDACTION:
//                handleDEVCARDACTION( /* isPractice, */ (SOCDevCardAction) mes);
//                break;
            
            /**
             * set the flag that tells if a player has played a
             * development card this turn
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.SETPLAYEDDEVCARD:
//                handleSETPLAYEDDEVCARD((SOCSetPlayedDevCard) mes);
//                break;
            
            /**
             * handle the change face message
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.CHANGEFACE:
//                handleCHANGEFACE((SOCChangeFace) mes);
//                break;
            
            /**
             * handle the reject connection message
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.REJECTCONNECTION:
//                handleREJECTCONNECTION((SOCRejectConnection) mes);
//                break;
            
            /**
             * handle the longest road message
             * handled by the base class, not overridden
             */
//            case SOCMessage.LONGESTROAD:
//                handleGAMEELEMENT(ga, GEType.LONGEST_ROAD_PLAYER, ((SOCLongestRoad) mes).getPlayerNumber());
//                break;
            
            /**
             * handle the largest army message
             * handled by the base class, not overridden
             */
//            case SOCMessage.LARGESTARMY:
//                handleGAMEELEMENT(ga, GEType.LARGEST_ARMY_PLAYER, ((SOCLargestArmy) mes).getPlayerNumber());
//                break;
            
            /**
             * handle the seat lock state message
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.SETSEATLOCK:
//                handleSETSEATLOCK((SOCSetSeatLock) mes);
//                break;
            
             /**
             * handle board reset (new game with same players, same game name, new layout).
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.RESETBOARDAUTH:
//                handleRESETBOARDAUTH((SOCResetBoardAuth) mes);
//                break;
            
             /**
             * handle updated game option info from server
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.GAMEOPTIONINFO:
//                handleGAMEOPTIONINFO((SOCGameOptionInfo) mes /*, isPractice */);
//                break;
            
               /**
             * All players' dice roll result resources.
             * Added 2013-09-20 for v2.0.00.
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.DICERESULTRESOURCES:
//                handleDICERESULTRESOURCES((SOCDiceResultResources) mes);
//                break;
            
            /**
             * move a previous piece (a ship) somewhere else on the board.
             * Added 2011-12-05 for v2.0.00.
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.MOVEPIECE:
//                handleMOVEPIECE((SOCMovePiece) mes);
//                break;
            
            /**
             * remove a piece (a ship) from the board in certain scenarios.
             * Added 2013-02-19 for v2.0.00.
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.REMOVEPIECE:
//                handleREMOVEPIECE((SOCRemovePiece) mes);
//                break;
            
             /**
             * reveal a hidden hex on the board.
             * Added 2012-11-08 for v2.0.00.
              * Overridden from {@link MessageHandler}, but calls super method
             */
//            case SOCMessage.REVEALFOGHEX:
//                handleREVEALFOGHEX( (SOCRevealFogHex) mes );
//                break;
            
            /**
             * update a village piece's value on the board (cloth remaining).
             * Added 2012-11-16 for v2.0.00.
             * Overridden from {@link MessageHandler}
             */
//            case SOCMessage.PIECEVALUE:
//                handlePIECEVALUE((SOCPieceValue) mes);
//                break;
            
        /** end overridden methods **/
                
             /*
                messages with different methods in the two classes
             */
            
            /**
              * Report robbery result.
              * Added 2020-09-15 for v2.5.00.
              * calls a dynamic method which then calls the static method
              */
            case SOCMessage.ROBBERYRESULT:
                handleROBBERYRESULT( (SOCRobberyResult) mes );
                break;
            
            /**
             * Player has Picked Resources.
             * Added 2020-12-14 for v2.5.00.
             * calls a dynamic method which then calls the static method
             */
            case SOCMessage.PICKRESOURCES:
                handlePICKRESOURCES( (SOCPickResources) mes );
                break;
            
            /**
             * Server has declined player's request.
             * Added 2021-12-08 for v2.5.00.
             * calls a dynamic method which does NOT call the static method
             */
            case SOCMessage.DECLINEPLAYERREQUEST:
                handleDECLINEPLAYERREQUEST( (SOCDeclinePlayerRequest) mes );
                break;
            
            /**
             * Update last-action data.
             * Added 2022-12-20 for v2.7.00.
             */
            case SOCMessage.SETLASTACTION:
                staticHandleSETLASTACTION( (SOCSetLastAction) mes,
                                           getClient().getGame( ((SOCSetLastAction) mes).getGameName() ) );
                break;
            
            /**
             * Reopen or close a shipping trade route.
             * Added 2022-12-18 for v2.7.00.
             */
            case SOCMessage.SETSHIPROUTECLOSED:
                staticHandleSETSHIPROUTECLOSED( (SOCSetShipRouteClosed) mes,
                                                getClient().getGame( ((SOCSetShipRouteClosed) mes).getGameName() ) );
                break;
            
            /**
             * join game authorization. Unique to this class
             */
            case SOCMessage.JOINGAMEAUTH:
                handleJOINGAMEAUTH( (SOCJoinGameAuth) mes /*, isPractice */ );
                break;
            
            /**
             * a player built something
             */
            case SOCMessage.PUTPIECE:       // TODO: reconcile
                // calls a dynamic method
                handlePUTPIECE( (SOCPutPiece) mes );
                break;
                
            /**
             * A player has discarded resources.
             * Added 2021-11-26 for v2.5.00.
             * Calls dynamic method which calls static super method
             */
            case SOCMessage.DISCARD:
                handleDISCARD( (SOCDiscard) mes );
                break;
            
            /**
             * a player has accepted a trade offer
             * calls dynamic method which then calls static method
             */
            case SOCMessage.ACCEPTOFFER:
                handleACCEPTOFFER( (SOCAcceptOffer) mes );
                break;
            
            /**
             * a player has made a bank/port trade
             * calls dynamic method which then calls static method
             */
            case SOCMessage.BANKTRADE:
                handleBANKTRADE( (SOCBankTrade) mes /* , isPractice */ );
                break;
            
            /**
             * update game data, like the current number of development cards
             */
            case SOCMessage.DEVCARDCOUNT:
                handleGAMEELEMENT( game, GEType.DEV_CARD_COUNT, ((SOCDevCardCount) mes).getNumDevCards() );
                break;
            
            /**
             * receive a list of all the potential settlements for a player
             * calls dynamic method which calls static super method
             */
            case SOCMessage.POTENTIALSETTLEMENTS:
                handlePOTENTIALSETTLEMENTS( (SOCPotentialSettlements) mes );
                break;
            
            /**
             * player statistics (as of 20100312 (v 1.1.09))
             */
            case SOCMessage.PLAYERSTATS:
                handlePLAYERSTATS( (SOCPlayerStats) mes );
                break;
            
            /**
             * generic "simple action" announcements from the server.
             * Added 2013-09-04 for v1.1.19.
             * Calls dynamic method which then calls static method
             */
            case SOCMessage.SIMPLEACTION:
                handleSIMPLEACTION( (SOCSimpleAction) mes );
                break;
            
            /**
             * Undo moving or placing a piece.
             * Added 2022-11-14 for v2.7.00.
             * calls dynamic method which then calls static method
             */
            case SOCMessage.UNDOPUTPIECE:
                handleUNDOPUTPIECE( (SOCUndoPutPiece) mes );
                break;
            
            /**
             * a special inventory item action: either add or remove,
             * or we cannot play our requested item.
             * Added 2013-11-26 for v2.0.00.
             * calls dynamic method which then calls static method
             */
            case SOCMessage.INVENTORYITEMACTION:
                handleINVENTORYITEMACTION( (SOCInventoryItemAction) mes );
                break;
            
            /**
             * Special Item change announcements.
             * Added 2014-04-16 for v2.0.00.
             * calls dynamic method which then calls static method
             */
            case SOCMessage.SETSPECIALITEM:
                handleSETSPECIALITEM( (SOCSetSpecialItem) mes );
                break;
                
            /*
                List of messages important to this class but ignored by the super class
             */
            
            /**
             * someone joined a chat channel
             */
            case SOCMessage.JOINCHANNEL:
                handleJOINCHANNEL( (SOCJoinChannel) mes );
                break;
            
            /**
             * list of members for a chat channel
             */
            case SOCMessage.CHANNELMEMBERS:
                handleCHANNELMEMBERS( (SOCChannelMembers) mes );
                break;
            
            /**
             * a new chat channel has been created
             */
            case SOCMessage.NEWCHANNEL:
                handleNEWCHANNEL( (SOCNewChannel) mes );
                break;
            
            /**
             * List of chat channels on the server: Server connection is complete.
             * (sent at connect after VERSION, even if no channels)
             * Show main panel if not already showing; see handleCHANNELS javadoc.
             * Not handled in the base class
             */
            case SOCMessage.CHANNELS:
                handleCHANNELS( (SOCChannels) mes );
                break;
            
            /**
             * text message to a chat channel
             * Not handled in the base class
             */
            case SOCMessage.CHANNELTEXTMSG:
                handleCHANNELTEXTMSG( (SOCChannelTextMsg) mes );
                break;
            
            /**
             * someone left the chat channel
             * Not handled in the base class
             */
            case SOCMessage.LEAVECHANNEL:
                handleLEAVECHANNEL( (SOCLeaveChannel) mes );
                break;
            
            /**
             * delete a chat channel
             * Not handled in the base class
             */
            case SOCMessage.DELETECHANNEL:
                handleDELETECHANNEL( (SOCDeleteChannel) mes );
                break;
            
            /**
             * list of games on the server
             * Not handled in the base class
             */
            case SOCMessage.GAMES:
                handleGAMES( (SOCGames) mes /*, isPractice */ );
                break;
            
            /**
             * list of games with options on the server
             * Not handled in the base class
             */
            case SOCMessage.GAMESWITHOPTIONS:
                handleGAMESWITHOPTIONS( (SOCGamesWithOptions) mes /*, isPractice */ );
                break;
            
            /**
             * someone joined a game
             * Not handled in the base class
             */
            case SOCMessage.JOINGAME:
                handleJOINGAME( (SOCJoinGame) mes );
                break;
            
            /**
             * new game has been created
             * Not handled in the base class
             */
            case SOCMessage.NEWGAME:
                handleNEWGAME( (SOCNewGame) mes /*, isPractice */ );
                break;
            
            /**
             * new game with options has been created
             * Not handled in the base class
             */
            case SOCMessage.NEWGAMEWITHOPTIONS:
                handleNEWGAMEWITHOPTIONS( (SOCNewGameWithOptions) mes /*, isPractice */ );
                break;
            
            /**
             * game has been destroyed
             * Not handled in the base class
             */
            case SOCMessage.DELETEGAME:
                handleDELETEGAME( (SOCDeleteGame) mes /*, isPractice )*/ );
                break;
            
            /**
             * list of game members
             * Not handled in the base class
             */
            case SOCMessage.GAMEMEMBERS:
                handleGAMEMEMBERS( (SOCGameMembers) mes );
                break;
            
            /**
             * game text message
             */
            case SOCMessage.GAMETEXTMSG:
                handleGAMETEXTMSG((SOCGameTextMsg) mes);
                break;
            
            /**
             * broadcast text message
             * Not handled in the base class
             */
            case SOCMessage.BCASTTEXTMSG:
                handleBCASTTEXTMSG( (SOCBCastTextMsg) mes );
                break;
            
            /**
             * prompt this player to discard
             * Not handled in the base class
             */
            case SOCMessage.DISCARDREQUEST:
                handleDISCARDREQUEST( (SOCDiscardRequest) mes );
                break;
            
            /**
             * prompt this player to choose a player to rob
             * Not handled in the base class
             */
            case SOCMessage.CHOOSEPLAYERREQUEST:
                handleCHOOSEPLAYERREQUEST( (SOCChoosePlayerRequest) mes );
                break;
            
            /**
             * a player has made an offer
             */
            case SOCMessage.MAKEOFFER:
                handleMAKEOFFER( (SOCMakeOffer) mes );
                break;
            
            /**
             * a player has rejected an offer
             * or server has disallowed our trade-related request.
             */
            case SOCMessage.REJECTOFFER:
                handleREJECTOFFER( (SOCRejectOffer) mes );
                break;
            
            /**
             * the trade message needs to be cleared
             */
            case SOCMessage.CLEARTRADEMSG:
                handleCLEARTRADEMSG( (SOCClearTradeMsg) mes );
                break;
            
            /**
             * game server text and announcements.
             * Added 2013-09-05 for v2.0.00.
             */
            case SOCMessage.GAMESERVERTEXT:
                handleGAMESERVERTEXT( (SOCGameServerText) mes );
                break;
                
        /*
            Methods for messages unique to this class
         */
            
            /**
             * Localized i18n strings for game items.
             * Added 2015-01-11 for v2.0.00.
             */
            case SOCMessage.LOCALIZEDSTRINGS:
                handleLOCALIZEDSTRINGS( (SOCLocalizedStrings) mes /*, isPractice */ );
                break;
            
            /**
             * Updated scenario info.
             * Added 2015-09-21 for v2.0.00.
             */
            case SOCMessage.SCENARIOINFO:
                handleSCENARIOINFO( (SOCScenarioInfo) mes /*, isPractice */ );
                break;
            
            /**
             * handle the roll dice prompt message
             * (it is now x's turn to roll the dice)
             */
            case SOCMessage.ROLLDICEPROMPT:
                handleROLLDICEPROMPT( (SOCRollDicePrompt) mes );
                break;
            
            /**
             * another player has voted on a board reset request
             */
            case SOCMessage.RESETBOARDVOTE:
                handleRESETBOARDVOTE( (SOCResetBoardVote) mes );
                break;
            
            /**
             * voting complete, board reset request rejected
             */
            case SOCMessage.RESETBOARDREJECT:
                handleRESETBOARDREJECT( (SOCResetBoardReject) mes );
                break;
            
            /**
             * for game options (1.1.07)
             */
            case SOCMessage.GAMEOPTIONGETDEFAULTS:
                handleGAMEOPTIONGETDEFAULTS( (SOCGameOptionGetDefaults) mes /*, isPractice )*/);
                break;
            
            /**
             * another player is requesting a board reset: we must vote
             */
            case SOCMessage.RESETBOARDVOTEREQUEST:
                handleRESETBOARDVOTEREQUEST( (SOCResetBoardVoteRequest) mes );
                break;
            
            /**
             * debug piece Free Placement (as of 20110104 (v 1.1.12))
             */
            case SOCMessage.DEBUGFREEPLACE:
                handleDEBUGFREEPLACE( (SOCDebugFreePlace) mes );
                break;
            
            /**
             * generic 'simple request' response from the server.
             * Added 2013-02-19 for v1.1.18.
             */
            case SOCMessage.SIMPLEREQUEST:
                handleSIMPLEREQUEST( (SOCSimpleRequest) mes );
                break;
            
            /**
             * Text that a player has been awarded Special Victory Point(s).
             * Added 2012-12-21 for v2.0.00.
             */
            case SOCMessage.SVPTEXTMSG:
                handleSVPTEXTMSG( (SOCSVPTextMessage) mes );
                break;
                
            /**
             * Prompt this player to choose to rob cloth or rob resources.
             * Added 2012-11-17 for v2.0.00.
             */
            case SOCMessage.CHOOSEPLAYER:
                handleCHOOSEPLAYER( (SOCChoosePlayer) mes );
                break;
                
            default:
                // if we didn't recognize the message, maybe our base class will...
                // TODO: are there any message we don't want the super class to handle?
                super.handle( mes, didDebugPrintAlready );
            }  // switch (mes.getType())
        }
        catch (Throwable th)
        {
            System.out.println( "MessageHandlernERROR - " + th.getMessage() );
            th.printStackTrace();
            System.out.println( "  For message: " + mes );
        }
    }

    /** END HANDLE CASE **/

    /**
     * Handle the "version" message, server's version report.
     * Ask server for game-option info if client's version differs.
     * Store the server's version for {@link soc.baseclient.SOCBaseClient#getServerVersion(SOCGame)}
     * and display the version on the main panel.
     * (Local server's version is always {@link Version#versionNumber()}.)
     *
     * @ param isPractice Is the server a local in-process server not remote?
     *                   Client can be connected only to one at a time.
     * @param mes  the message
     * @since 1.1.00
     */
    @Override
    protected void handleVERSION( /* boolean isPractice, */ SOCVersion mes)
    {
        D.ebugPrintlnINFO("handleVERSION: " + mes);
        int vers = mes.getVersionNumber();
        
        SOCPlayerClient playerClient = getClient();
//        boolean isPractice = false;
//        if (! isPractice)
        {
            playerClient.sVersion = vers;
            playerClient.sFeatures = (vers >= SOCFeatureSet.VERSION_FOR_SERVERFEATURES)
                    ? new SOCFeatureSet(mes.feats)
                    : new SOCFeatureSet(true, true);
            
            playerClient.getMainDisplay().showVersion(vers, mes.getVersionString(), mes.getBuild(), getClient().sFeatures);
        }

        // If we ever require a minimum server version, would check that here.

        // Check known game options vs server's version. (added in 1.1.07)
        // Server's responses will add, remove or change our "known options".
        // In v2.0.00 and later, also checks for game option localized descriptions.
        // In v2.5.00 and later, also checks for 3rd-party game opts.

        final int cliVersion = Version.versionNumber();
        final boolean sameVersion = (getClient().sVersion == cliVersion);
        final Locale clientLocale = getClient().getClientLocale();
        final boolean withTokenI18n =
               ( clientLocale != null)
            && ((playerClient.sVersion >= SOCStringManager.VERSION_FOR_I18N))
            && ! ("en".equals( clientLocale.getLanguage())
            && "US".equals( clientLocale.getCountry()));
        SOCGameOptionSet opts3p =
            ((playerClient.sVersion >= cliVersion) /* && ! isPractice */ )
            ? playerClient.tcpServGameOpts.knownOpts.optionsWithFlag(SOCGameOption.FLAG_3RD_PARTY, 0)
            : null;   // sVersion < cliVersion, so SOCGameOptionSet.optionsNewerThanVersion will find any 3rd-party opts

        if (    (/* (! isPractice) && */ (getClient().sVersion > cliVersion))
             || ((/* isPractice || */ sameVersion) && (withTokenI18n || (opts3p != null))) )
        {
            // Newer server: Ask it to list any options we don't know about yet.
            // Same version: Ask for all localized option descs if available.
            // Also ask about any 3rd-party options known at getClient().

            final SOCGameOptionGetInfos ogiMsg;
            if (opts3p != null)
            {
                ArrayList<String> olist = new ArrayList<>(opts3p.keySet());
                if (! sameVersion)
                    olist.add(SOCGameOptionGetInfos.OPTKEY_GET_ANY_CHANGES);
                ogiMsg = new SOCGameOptionGetInfos(olist, withTokenI18n, false);
                    // sends opts and maybe "?I18N"
            }
            else
            {
                ogiMsg = new SOCGameOptionGetInfos(null, withTokenI18n, withTokenI18n && sameVersion);
                    // sends "-" and/or "?I18N"
            }

//            if (! isPractice)     // always true for TCPMessageHandler
            playerClient.getMainDisplay().optionsRequested();
            playerClient.sendMessage( ogiMsg.toCmd() );
        }
        else if ((playerClient.sVersion < cliVersion) /* && ! isPractice */)
        {
            if (playerClient.sVersion >= SOCNewGameWithOptions.VERSION_FOR_NEWGAMEWITHOPTIONS)
            {
                // Older server: Look for options created or changed since server's version
                // (and any 3rd-party options).
                // Ask it what it knows about them.

                SOCGameOptionSet knownOpts = playerClient.tcpServGameOpts.knownOpts;
                if (knownOpts == null)
                {
                    knownOpts = SOCGameOptionSet.getAllKnownOptions();
                    playerClient.tcpServGameOpts.knownOpts = knownOpts;
                }

                List<SOCGameOption> tooNewOpts =
                    knownOpts.optionsNewerThanVersion(playerClient.sVersion, false, false);

                if ((tooNewOpts != null) && (playerClient.sVersion < SOCGameOption.VERSION_FOR_LONGER_OPTNAMES))
                {
                    // Server is older than 2.0.00; we can't send it any long option names.
                    // Remove them from our set of options usable for games on that server.

                    Iterator<SOCGameOption> opi = tooNewOpts.iterator();
                    while (opi.hasNext())
                    {
                        final SOCGameOption op = opi.next();
                        //TODO i18n how to?
                        if ((op.key.length() > 3) || op.key.contains("_"))
                        {
                            knownOpts.remove(op.key);
                            opi.remove();
                        }
                    }
                    if (tooNewOpts.isEmpty())
                        tooNewOpts = null;
                }

                if (tooNewOpts != null)
                {
                    playerClient.getMainDisplay().optionsRequested();
                    playerClient.sendMessage( new SOCGameOptionGetInfos(tooNewOpts, withTokenI18n).toCmd() );
//                    gms.put(new SOCGameOptionGetInfos(tooNewOpts, withTokenI18n).toCmd(), isPractice);
                }
                else if (withTokenI18n)
                {
                    // server is older than client but understands i18n: request gameopt localized strings
                    playerClient.sendMessage( new SOCGameOptionGetInfos(null, true, false).toCmd() );  // sends opt list "-,?I18N"
                }
            } 
            else 
            {
                // server is too old to understand options. Can't happen with local practice srv,
                // because that's our version (it runs from our own JAR file).
                playerClient.tcpServGameOpts.noMoreOptions(true);
                playerClient.tcpServGameOpts.knownOpts = null;
            }
        }
        else 
        {          
            // playerClient.sVersion == cliVersion, so we have same info/code as server for getAllKnownOptions,
            // scenarios, etc. and found nothing else to ask about (i18n, 3rd-party gameopts).

            // For practice games, knownOpts may already be initialized, so check vs null.
            ServerGametypeInfo opts = (/* isPractice ? ((SOCFullClient)client).practiceServGameOpts : */ playerClient.tcpServGameOpts);
            if (opts.knownOpts == null)
                opts.knownOpts = SOCGameOptionSet.getAllKnownOptions();
            opts.noMoreOptions( false /* isPractice*/ );  // defaults not known unless it's practice

            if (! (withTokenI18n /* || isPractice */ ))
            {
                // won't need i18n strings: set flags so we won't ask server later for scenario details
                opts.allScenStringsReceived = true;
                opts.allScenInfoReceived = true;
            }
        }
    }

    /**
     * handle the {@link SOCStatusMessage "status"} message.
     * Used for server events, also used if player tries to join a game
     * but their nickname is not OK.
     *<P>
     * Also used (v1.1.19 and newer) as a reply to {@link SOCAuthRequest} sent
     * before showing {@link NewGameOptionsFrame}, so check whether the
     * {@link SOCPlayerClient#isNGOFWaitingForAuthStatus isNGOFWaitingForAuthStatus getClient().isNGOFWaitingForAuthStatus}
     * flag is set.
     *
     * @param mes  the message
     * @ param isPractice from practice server, not remote server?
     */
    @Override
    protected void handleSTATUSMESSAGE(SOCStatusMessage mes /*, final boolean isPractice */ )
    {
        int sv = mes.getStatusValue();
        String statusText = mes.getStatus();

        if ((sv == SOCStatusMessage.SV_OK_SET_NICKNAME))
        {
            sv = SOCStatusMessage.SV_OK;

            final int i = statusText.indexOf(SOCMessage.sep2_char);
            if (i > 0)
            {
                getClient().setNickname( statusText.substring(0, i) );
                statusText = statusText.substring(i + 1);
                getClient().getMainDisplay().setNickname(getClient().getNickname());

                // SV_OK_SET_NICKNAME won't ever come from to the practice server;
                // leave getClient().practiceNickname unchanged.
            }
        }

        final boolean srvDebugMode;
        if ( getClient().sVersion >= 2000)
        {
            final boolean svIsOKDebug = (sv == SOCStatusMessage.SV_OK_DEBUG_MODE_ON);
            srvDebugMode = svIsOKDebug;
            if (svIsOKDebug)
                sv = SOCStatusMessage.SV_OK;
        }
        else
        {
            srvDebugMode = statusText.toLowerCase().contains("debug");
        }

        getClient().getMainDisplay().showStatus(statusText, (sv == SOCStatusMessage.SV_OK), srvDebugMode);

        // Are we waiting for auth response in order to show NGOF?
        if (/* (! isPractice) && */ getClient().isNGOFWaitingForAuthStatus)
        {
            getClient().isNGOFWaitingForAuthStatus = false;

            if (sv == SOCStatusMessage.SV_OK)
            {
                getClient().setAuthenticated( true );

                EventQueue.invokeLater( () -> getClient().getMainDisplay().gameWithOptionsBeginSetup(false, true) );
            }
        }

        switch (sv)
        {
        case SOCStatusMessage.SV_PW_WRONG:
            getClient().getMainDisplay().focusPassword();
            break;

        case SOCStatusMessage.SV_NEWGAME_OPTION_VALUE_TOONEW:
        {
            // Extract game name and failing game-opt keynames,
            // and pop up an error message window.
            String errMsg;
            StringTokenizer st = new StringTokenizer(statusText, SOCMessage.sep2);
            try
            {
                String gameName;
                ArrayList<String> optNames = new ArrayList<String>();
                errMsg = st.nextToken();
                gameName = st.nextToken();
                while (st.hasMoreTokens())
                    optNames.add(st.nextToken());

                StringBuilder opts = new StringBuilder();
                final SOCGameOptionSet knowns = getClient().tcpServGameOpts.knownOpts;
//                    (isPractice) ? getClient().practiceServGameOpts.knownOpts : getClient().tcpServGameOpts.knownOpts;
                for (String oname : optNames)
                {
                    opts.append('\n');
                    SOCGameOption oinfo = null;
                    if (knowns != null)
                        oinfo = knowns.get(oname);
                    if (oinfo != null)
                        oname = oinfo.getDesc();
                    opts.append(getClient().strings.get("options.error.valuesproblem.which", oname));
                }
                errMsg = getClient().strings.get("options.error.valuesproblem", gameName, errMsg, opts.toString());
            }
            catch (Throwable t)
            {
                errMsg = statusText;  // fallback, not expected to happen
            }

            getClient().getMainDisplay().showErrorDialog(errMsg, getClient().strings.get("base.cancel"));
        }
        break;

        case SOCStatusMessage.SV_GAME_CLIENT_FEATURES_NEEDED:
        {
            // Extract game name and missing client feature keynames,
            // and pop up an error message window.
            String errMsg;
            StringTokenizer st = new StringTokenizer(statusText, SOCMessage.sep2);
            try
            {
                st.nextToken();     // skip the first token
                final String gameName = st.nextToken();
                final String featsList = (st.hasMoreTokens()) ? st.nextToken() : "?";
                final String msgKey = (getClient().doesGameExist(gameName, true))
                    ? "pcli.gamelist.client_feats.cannot_join"
                        // "Cannot create game {0}\nThis client does not have required feature(s): {1}"
                    : "pcli.gamelist.client_feats.cannot_create";
                        // "Cannot join game {0}\nThis client does not have required feature(s): {1}"
                errMsg = getClient().strings.get(msgKey, gameName, featsList);
            }
            catch (Throwable t)
            {
                errMsg = statusText;  // fallback, not expected to happen
            }

            getClient().getMainDisplay().showErrorDialog(errMsg, getClient().strings.get("base.cancel"));
        }
        break;

        case SOCStatusMessage.SV_SERVER_SHUTDOWN:
        {
            handleBCASTTEXTMSG(statusText);
            getClient().socketConnection.setLastException( new RuntimeException(statusText) );
            getClient().shutdownFromNetwork();
        }
        break;
        }
    }
    
    /**
     * echo the server ping, to ensure we're still connected.
     * Ping may be a keepalive check or an attempt to kick by another
     * client with the same nickname; may call
     * {@link SOCPlayerClient#shutdownFromNetwork()} if so.
     *<P>
     * (message ignored before v1.1.08)
     * @since 1.1.08
     */
    @Override
    protected void handleSERVERPING( SOCServerPing mes /*, final boolean isPractice */ )
    {
        int timeval = mes.getSleepTime();
        if (timeval != -1)
        {
            getClient().sendMessage( mes.toCmd() );
        }
        else
        {
            // if timeval is < 0, we are being kicked by player with same name
            getClient().socketConnection.setLastException(
                    new RuntimeException(getClient().strings.get("pcli.error.kicked.samename")));
            getClient().shutdownFromNetwork();
        }
    }
    
    /**
     * handle the "join channel" message
     * @param mes  the message
     */
    protected void handleJOINCHANNEL(SOCJoinChannel mes)
    {
        getClient().getMainDisplay().channelJoined(mes.getChannel(), mes.getNickname());
    }

    /**
     * handle the "channel members" message
     * @param mes  the message
     */
    protected void handleCHANNELMEMBERS(SOCChannelMembers mes)
    {
        getClient().getMainDisplay().channelMemberList(mes.getChannel(), mes.getMembers());
    }

    /**
     * handle the "new channel" message
     * @param mes  the message
     */
    protected void handleNEWCHANNEL(SOCNewChannel mes)
    {
        getClient().getMainDisplay().channelCreated(mes.getChannel());
    }

    /**
     * handle the "list of channels" message; this message indicates that
     * we're newly connected to the server, and is sent even if the server
     * isn't using {@link SOCFeatureSet#SERVER_CHANNELS}: Server connection is complete.
     * Unless {@code isPractice}, show {@link MainDisplay}.
     * @param mes  the message
     */
    protected void handleCHANNELS(final SOCChannels mes )
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                MainDisplay mdisp = getClient().getMainDisplay();
                mdisp.channelList(mes.getChannels(), false );
                mdisp.repaintGameAndChannelLists();
            }
        });
    }
    
    /**
     * handle the "join channel authorization" message
     * @param mes  the message
     */
    @Override
    protected void handleJOINCHANNELAUTH(SOCJoinChannelAuth mes)
    {
        getClient().setAuthenticated( true );
        getClient().getMainDisplay().channelJoined(mes.getChannel());
    }
    
    /**
     * Handle a broadcast text message. Calls {@link #handleBCASTTEXTMSG(String)}.
     * @param mes  the message
     */
    protected void handleBCASTTEXTMSG(SOCBCastTextMsg mes)
    {
        handleBCASTTEXTMSG(mes.getText());
    }

    /**
     * Handle a broadcast message containing text as if it were {@link SOCBCastTextMsg}.
     * @param txt  the message text
     * @since 2.1.00
     */
    protected void handleBCASTTEXTMSG(final String txt)
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                getClient().getMainDisplay().chatMessageBroadcast(txt);

                for (PlayerClientListener pcl : getClient().getClientListeners().values())
                    pcl.messageBroadcast(txt);
            }
        });
    }

    /**
     * handle a text message received in a channel
     * @param mes  the message
     * @see #handleGAMETEXTMSG(SOCGameTextMsg)
     */
    protected void handleCHANNELTEXTMSG(SOCChannelTextMsg mes)
    {
        getClient().getMainDisplay().chatMessageReceived(mes.getChannel(), mes.getNickname(), mes.getText());
    }

    /**
     * handle the "leave channel" message
     * @param mes  the message
     */
    protected void handleLEAVECHANNEL(SOCLeaveChannel mes)
    {
        getClient().getMainDisplay().channelLeft(mes.getChannel(), mes.getNickname());
    }

    /**
     * handle the "delete channel" message
     * @param mes  the message
     */
    protected void handleDELETECHANNEL(SOCDeleteChannel mes)
    {
        final String chName = mes.getChannel();

        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                getClient().getMainDisplay().channelDeleted(chName);
            }
        });
    }

    /**
     * handle the "list of games" message
     * @param mes  the message
     */
    protected void handleGAMES(final SOCGames mes /* , final boolean isPractice */ )
    {
        // Any game's name in this msg may start with the "unjoinable" prefix
        // SOCGames.MARKER_THIS_GAME_UNJOINABLE.
        // We'll recognize and remove it in methods called from here.

        List<String> gameNames = mes.getGames();
        
        if (getClient().serverGames == null)
            getClient().serverGames = new SOCGameList(getClient().tcpServGameOpts.knownOpts);
        getClient().serverGames.addGames(gameNames, Version.versionNumber());

        // No more game-option info will be received,
        // because that's always sent before game names are sent.
        // We may still ask for GAMEOPTIONGETDEFAULTS if asking to create a game,
        // but that will happen when user clicks that button, not yet.
        getClient().tcpServGameOpts.noMoreOptions(false);

        // update displayed list on AWT event thread, not network message thread,
        // to ensure right timing for repaint to avoid appearing empty.
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                for (String gn : mes.getGames())
                    getClient().addToGameList(gn, null, true);

                getClient().getMainDisplay().repaintGameAndChannelLists();
            }
        });
    }

    /** start handleJOINGAMEAUTH **/
    
    /**
     * handle the "join game authorization" message: create new {@link SOCGame} and
     * {@link SOCPlayerInterface} so user can join the game
     * @param mes  the message
     * @ param isPractice  if server is practiceServer (not normal tcp network)
     * @throws IllegalStateException if board size {@link SOCGameOption} "_BHW" isn't defined (unlikely internal error)
     */
    protected void handleJOINGAMEAUTH(SOCJoinGameAuth mes /*, final boolean isPractice */ )
        throws IllegalStateException
    {
            getClient().setAuthenticated( true );

        final SOCGameOptionSet knownOpts = getClient().tcpServGameOpts.knownOpts;

        final String gaName = mes.getGameName();
        SOCGameOptionSet gameOpts;
        if (getClient().serverGames != null)
            gameOpts = getClient().serverGames.parseGameOptions(gaName);
        else
            gameOpts = null;

        final int bh = mes.getBoardHeight(), bw = mes.getBoardWidth();
        if ((bh != 0) || (bw != 0))
        {
            // Encode board size to pass through game constructor.
            // gameOpts won't be null, because bh, bw are from SOCBoardLarge which requires a gameopt to use.
            SOCGameOption opt = knownOpts.getKnownOption("_BHW", true);
            if (opt == null)
                throw new IllegalStateException("Internal error: Game opt _BHW not known");
            opt.setIntValue((bh << 8) | bw);
            if (gameOpts == null)
                gameOpts = new SOCGameOptionSet();  // unlikely: no-opts board has 0 height,width in message
            gameOpts.put(opt);
        }

        SOCGame ga = new SOCGame(gaName, gameOpts, knownOpts);
        ga.isPractice = false;
        ga.serverVersion = getClient().sVersion;

        PlayerClientListener clientListener =
            getClient().getMainDisplay().gameJoined(ga, mes.getLayoutVS(), getClient().getGameReqLocalPrefs().get(gaName));
        getClient().getClientListeners().put(gaName, clientListener);
        getClient().addGame( ga );
    }

    /**
     * handle the "join game" message
     * @param mes  the message
     */
    protected void handleJOINGAME(SOCJoinGame mes)
    {
        final String gn = mes.getGame();
        final String name = mes.getNickname();
        if (name == null)
            return;

        PlayerClientListener pcl = getClient().getClientListener(gn);
        pcl.playerJoined(name);
    }

    /**
     * handle the "leave game" message
     * @param mes  the message
     */
    @Override
    protected void handleLEAVEGAME(SOCLeaveGame mes)
    {
        String gameName = mes.getGameName();
        SOCGame game = getClient().getGame(gameName );
        if (game == null)
            return;

        final String name = mes.getNickname();
        final SOCPlayer player = game.getPlayer(name);

        // Give the listener a chance to clean up while the player is still in the game
        PlayerClientListener pcl = getClient().getClientListener( gameName );
        pcl.playerLeft(name, player);

        if (player != null)
        {
            //  This user was not a spectator.
            //  Remove first from listener, then from game data.
            game.removePlayer(name, false);
        }
    }

    /**
     * handle the "new game" message
     * @param mes  the message
     */
    protected void handleNEWGAME(SOCNewGame mes /*, final boolean isPractice */ )
    {
        // Run in network message thread, not AWT event thread,
        // in case client is about to be auth'd to join this game:
        // messages must take effect in the order sent

        getClient().addToGameList(mes.getGameName(), null, true /* ! isPractice */ );
    }

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    protected void handleDELETEGAME(SOCDeleteGame mes /*, final boolean isPractice */ )
    {
        final String gaName = mes.getGameName();

        // run on AWT event thread, not network thread, to avoid occasional ArrayIndexOutOfBoundsException
        // console stack trace (javax.swing.DefaultListModel.getElementAt) after deleteFromGameList

        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                final MainDisplay mdisp = getClient().getMainDisplay();
                if (! mdisp.deleteFromGameList(gaName, false /* isPractice */, false))
                    mdisp.deleteFromGameList(gaName,  false /* isPractice */, true);

                PlayerClientListener pcl = getClient().getClientListener(gaName);
                if (pcl != null)
                    pcl.gameDisconnected(true, null);
            }
        });
    }

    /**
     * handle the "game members" message, the server's hint that it's almost
     * done sending us the complete game state in response to JOINGAME.
     * @param mes  the message
     */
    protected void handleGAMEMEMBERS(final SOCGameMembers mes)
    {
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.membersListed(mes.getMembers());
    }

    /**
     * handle the "game stats" message.
     */
    @Override
    protected void handleGAMESTATS(SOCGameStats mes)
    {
        final String gaName = mes.getGameName();
        final int stype = mes.getStatType();

        SOCGame ga = getClient().getGame(gaName);
        if (ga != null)
            super.handleGAMESTATS(mes);

        switch (stype)
        {
        case SOCGameStats.TYPE_PLAYERS:
            // If we're playing in a game, update the scores. (SOCPlayerInterface)
            // This is used to show the true scores, including hidden
            // victory-point cards, at the game's end.
            if (ga != null)
                getClient().updateGameEndStats(gaName, mes.getScores());
            break;

        case SOCGameStats.TYPE_TIMING:
            /** similar logic is in {@link SOCDisplaylessMessageHandler#handleGAMESTATS **/

            final long[] stats = mes.getScores();
            final long timeCreated = stats[0], timeFinished = stats[2],
                timeNow = System.currentTimeMillis() / 1000;
            if (timeCreated > timeNow)
                return;
            final int durationFinishedSeconds =
                (timeFinished > timeCreated) ? ((int) (timeFinished - timeCreated)) : 0;

            getClient().getMainDisplay().gameTimingStatsReceived
                (gaName, timeCreated, (stats[1] == 1), durationFinishedSeconds);
            break;
        }
    }

    /**
     * handle the "game text message" message.
     * Messages not from Server go to the chat area.
     * Messages from Server go to the game text window.
     * Urgent messages from Server (starting with ">>>") also go to the chat area,
     * which has less activity, so they are harder to miss.
     *
     * @param mes  the message
     * @see #handleGAMESERVERTEXT(SOCGameServerText)
     * @see #handleCHANNELTEXTMSG(SOCChannelTextMsg)
     * unique to this PlayerMessageHandler
     */
    protected void handleGAMETEXTMSG(SOCGameTextMsg mes)
    {
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;

        String fromNickname = mes.getNickname();
        if (fromNickname.equals(SOCGameTextMsg.SERVERNAME))  // for pre-2.0.00 servers not using SOCGameServerText
            fromNickname = null;
        pcl.messageReceived(fromNickname, mes.getText());
    }

    /**
     * handle the "player sitting down" message
     * @param mes  the message
     */
    @Override
    protected SOCGame handleSITDOWN(final SOCSitDown mes)
    {
        /**
         * tell the game that a player is sitting
         */
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return null;

        final int pn = mes.getPlayerNumber();
        final String plName = mes.getNickname();
        SOCPlayer player;

        ga.takeMonitor();
        try
        {
            ga.addPlayer(plName, pn);
            player = ga.getPlayer(pn);
            player.setRobotFlag(mes.isRobot(), false);
        }
        catch (Exception e)
        {
            System.out.println("Exception caught - " + e);
            e.printStackTrace();

            return null;
        }
        finally
        {
            ga.releaseMonitor();
        }

        final boolean playerIsClient = getClient().getNickname().equals(plName);

        if (   playerIsClient
            && (ga.isPractice || (getClient().sVersion >= SOCDevCardAction.VERSION_FOR_SITDOWN_CLEARS_INVENTORY)))
        {
            // server is about to send our dev-card inventory contents
            player.getInventory().clear();
        }

        /**
         * tell the GUI that a player is sitting
         */
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.playerSitdown(pn, plName);

        /**
         * if player is client, use face icon from last requested change instead of default
         * (this is so that an old face isn't requested anew); skip if reset.
         */
        if (playerIsClient && ! ga.isBoardReset())
        {
            int id = getClient().lastFaceChange;
            player.setFaceId( id );
            // TODO: this seems repetitive
            getClient().lastFaceChange = id;
            getClient().sendMessage(new SOCChangeFace(ga.getName(), 0, id).toCmd() /*, ga.isPractice */ );
        }
        return ga;
    }

    /**
     * Handle the old "board layout" message (original 4-player board, no options).
     * Most game boards will call {@link #handleBOARDLAYOUT2(SOCBoardLayout2)} instead.
     * @param mes  the message
     */
    @Override
    protected void handleBOARDLAYOUT(SOCBoardLayout mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;

        super.handleBOARDLAYOUT(mes);

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl != null)
            pcl.boardLayoutUpdated();
    }

     /** Start handleBOARDLAYOUT2 **/
    
    /**
     * Handle the "board layout" message, in its usual format.
     * (Some simple games can use the old {@link #handleBOARDLAYOUT(SOCBoardLayout)} instead.)
     * @param mes  the message
     * @since 1.1.08
     */
    @Override
    protected boolean handleBOARDLAYOUT2(SOCBoardLayout2 mes)
    {
        String gaName = mes.getGameName();
        if (super.handleBOARDLAYOUT2( mes ))
        {
            PlayerClientListener pcl = getClient().getClientListener(gaName);
            if (pcl != null)
                pcl.boardLayoutUpdated();
        }
        return true;
    }

    /**
     * handle the "start game" message
     * calls {@link #handleGAMESTATE(SOCGame, int, boolean, boolean)}
     * which will call {@link PlayerClientListener#gameStarted()} if needed.
     * @param mes  the message
     */
    @Override
    protected void handleSTARTGAME( SOCStartGame mes )
    {
        final SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        handleGAMESTATE(ga, mes.getGameState(), false, false);
    }

    /**
     * Handle the "game state" message; calls {@link #handleGAMESTATE(SOCGame, int, boolean, boolean)}.
     * @param mes  the message
     */
    @Override
    protected void handleGAMESTATE(SOCGameState mes)
    {
        SOCGame ga = getClient().getGame( mes.getGameName() );
        if (ga != null)
            handleGAMESTATE(ga, mes.getState(), false, false);
    }

    /**
     * Handle game state message: Update {@link SOCGame} and {@link PlayerClientListener} if any.
     * Call for any message type which contains a Game State field.
     *<P>
     * Checks current {@link SOCGame#getGameState()}; if current state is {@link SOCGame#NEW NEW}
     * and {@code newState != NEW}, calls {@link PlayerClientListener#gameStarted()} before
     * its usual {@link PlayerClientListener#gameStateChanged(int, boolean)} call.
     *<P>
     * If current state is &lt; {@link SOCGame#ROLL_OR_CARD} and {@code newState == ROLL_OR_CARD},
     * and server older than v2.5.00 hasn't sent {@link SOCTurn} to increment {@link SOCGame#getRoundCount()} from 0,
     * calls {@link SOCGame#updateAtTurn()} to do so.
     *
     * @param ga  Game to update state; not null
     * @param newState  New state from message, like {@link SOCGame#ROLL_OR_CARD}, or 0. Does nothing if 0.
     * @param isForTurn  True if being called from {@link #handleTURN(SOCTurn)}.
     *     If true, will avoid calling {@link SOCGame#updateAtTurn()}; caller does so instead.
     * @param isForDecline  True if being called from {@link #handleDECLINEPLAYERREQUEST(SOCDeclinePlayerRequest)}.
     *     If true, won't call {@link SOCGame#setGameState(int)} if {@code newState} same as current state.
     *     Also passed to {@code PlayerClientListener.gameStateChanged(..)}.
     * @see #handleGAMESTATE(SOCGameState)
     * @since 2.0.00
     */
    private void handleGAMESTATE( final SOCGame ga, final int newState, final boolean isForTurn, final boolean isForDecline)
    {
        if (newState == 0)
            return;

        final int gaState = ga.getGameState();
        final boolean gameStarting = (gaState == SOCGame.NEW) && (newState != SOCGame.NEW);

        if ((newState != gaState) || ! isForDecline)
            ga.setGameState(newState);

        if ((! (gameStarting || isForTurn))
            && (gaState < SOCGame.ROLL_OR_CARD) && (newState == SOCGame.ROLL_OR_CARD)
            && (ga.getRoundCount() == 0))
        {
            // Servers older than v2.5.00 didn't always send SOCTurn before game's first turn
            // (SOCTurn.VERSION_FOR_SEND_BEGIN_FIRST_TURN)
            ga.updateAtTurn();
        }

        PlayerClientListener pcl = getClient().getClientListener(ga.getName());
        if (pcl == null)
            return;

        if (gameStarting)
        {
            // call here, not in handleSTARTGAME, in case we joined a game in progress
            pcl.gameStarted();
        }
        pcl.gameStateChanged(newState, false);
    }
    
    /**
     * Update one game element field from a {@link SOCGameElements} message,
     * then update game's {@link PlayerClientListener} display if appropriate.
     *<P>
     * To update game information, calls
     * {@link MessageHandler#staticHandleGAMEELEMENT(SOCGame, GEType, int)}.
     * <p/>
     * @param ga   Game to update; does nothing if null
     * @param etype  Element type, such as {@link GEType#ROUND_COUNT} or {@link GEType#DEV_CARD_COUNT}.
     *     Does nothing if {@code null}.
     * @param value  The new value to set
     * @since 2.0.00
     */
    @SuppressWarnings("incomplete-switch")
    protected void handleGAMEELEMENT( SOCGame ga, GEType etype, int value )
    {
        if ((ga == null) || (etype == null))
            return;
        
        final PlayerClientListener pcl = getClient().getClientListener(ga.getName());
        
        // A few etypes need to give PCL the old and new values.
        // For those, update game state and PCL together and return.
        if (pcl != null)
        {
            switch (etype)
            {
            // SOCGameElements.GEType.ROUND_COUNT:
            // Doesn't need a case here because it's sent only during joingame;
            // SOCBoardPanel will check ga.getRoundCount() as part of joingame
            
            case LARGEST_ARMY_PLAYER:
            {
                SOCPlayer oldLargestArmyPlayer = ga.getPlayerWithLargestArmy();
                staticHandleGAMEELEMENT(ga, etype, value);
                SOCPlayer newLargestArmyPlayer = ga.getPlayerWithLargestArmy();
                
                // Update player victory points; check for and announce change in largest army
                pcl.largestArmyRefresh(oldLargestArmyPlayer, newLargestArmyPlayer);
                
                return;
            }
            
            case LONGEST_ROAD_PLAYER:
            {
                SOCPlayer oldLongestRoadPlayer = ga.getPlayerWithLongestRoad();
                staticHandleGAMEELEMENT(ga, etype, value);
                SOCPlayer newLongestRoadPlayer = ga.getPlayerWithLongestRoad();
                
                // Update player victory points; check for and announce change in longest road
                pcl.longestRoadRefresh(oldLongestRoadPlayer, newLongestRoadPlayer);
                
                return;
            }
            }
        }
        
        staticHandleGAMEELEMENT(ga, etype, value);
        
        if (pcl == null)
            return;
        
        switch (etype)
        {
        case DEV_CARD_COUNT:
            pcl.devCardDeckUpdated();
            break;
        
        case CURRENT_PLAYER:
            pcl.playerTurnSet(value);
            break;
        }
    }
    
    /**
     * handle the "turn" message
     * @param mes  the message
     */
    @Override
    protected void handleTURN(SOCTurn mes)
    {
        final String gaName = mes.getGameName();
        SOCGame game = getClient().getGame(gaName);
        if (game == null)
            return;

        handleGAMESTATE(game, mes.getGameState(), true, false);

        final int pnum = mes.getPlayerNumber();
        game.setCurrentPlayerNumber(pnum);
        game.updateAtTurn();
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.playerTurnSet(pnum);
    }

    /** start  handlePLAYERELEMENT **/
    
    /**
     * handle the "player information" message: Finds game and its {@link PlayerClientListener} by name
     * and calls {@link #handlePLAYERELEMENT(PlayerClientListener, SOCGame, SOCPlayer, int, int, PEType, int, boolean)}.
     *<P>
     * To update game information, that method defaults to calling
     * {@link super#handlePLAYERELEMENT_simple(SOCGame, SOCPlayer, int, int, PEType, int, String)}
     * or {@link super#handlePLAYERELEMENT_numRsrc(SOCPlayer, int, int, int)}
     * for elements that don't need special handling.
     *
     * @param mes  the message
     */
    @Override
    protected void handlePLAYERELEMENT(SOCPlayerElement mes)
    {
        final SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        final int pn = mes.getPlayerNumber();
        final int action = mes.getAction(), amount = mes.getAmount();
        final PEType etype = PEType.valueOf(mes.getElementType());

        handlePLAYERELEMENT( getClient().getClientListener(mes.getGameName()),
                             ga, null, pn, action, etype, amount, mes.isNews());
    }

    /**
     * Handle a player information update from a {@link SOCPlayerElement} or {@link SOCPlayerElements} message.
     * Update game information, then update {@code pcl} display if appropriate.
     *<P>
     * To update game information, defaults to calling
     * {@link MessageHandler#handlePLAYERELEMENT_simple(SOCGame, SOCPlayer, int, int, PEType, int, String)}
     * or {@link MessageHandler#handlePLAYERELEMENT_numRsrc(SOCPlayer, int, int, int)}
     * for elements that don't need special handling for this client class.
     *
     * @param pcl  PlayerClientListener for {@code ga}, to update display if not null
     * @param ga   Game to update; does nothing if null
     * @param pl   Player to update; some elements take null. If null and {@code pn != -1}, will find {@code pl}
     *     using {@link SOCGame#getPlayer(int) ga.getPlayer(pn)}.
     * @param pn   Player number from message (sometimes -1 for none or all)
     * @param action   {@link SOCPlayerElement#SET}, {@link SOCPlayerElement#GAIN GAIN},
     *     or {@link SOCPlayerElement#LOSE LOSE}
     * @param etype  Element type, such as {@link PEType#SETTLEMENTS} or {@link PEType#NUMKNIGHTS}.
     *     Does nothing if {@code null}.
     * @param amount  The new value to set, or the delta to gain/lose
     * @param isNews  True if message's isNews() flag is set; used when calling
     *     {@link PlayerClientListener#playerElementUpdated(SOCPlayer, soc.client.PlayerClientListener.UpdateType, boolean, boolean)}
     * @since 2.0.00
     */
    private void handlePLAYERELEMENT( PlayerClientListener pcl, final SOCGame ga, SOCPlayer pl, final int pn,
         int action, PEType etype, int amount, boolean isNews)
    {
        if ((ga == null) || (etype == null))
            return;
        if ((pl == null) && (pn != -1))
            pl = ga.getPlayer(pn);

        PlayerClientListener.UpdateType utype = null;  // If not null, update this type's amount display

        switch (etype)
        {
        case ROADS:
            staticHandlePLAYERELEMENT_numPieces( pl, action, SOCPlayingPiece.ROAD, amount);
            utype = PlayerClientListener.UpdateType.Road;
            break;

        case SETTLEMENTS:
            staticHandlePLAYERELEMENT_numPieces(pl, action, SOCPlayingPiece.SETTLEMENT, amount);
            utype = PlayerClientListener.UpdateType.Settlement;
            break;

        case CITIES:
            staticHandlePLAYERELEMENT_numPieces( pl, action, SOCPlayingPiece.CITY, amount);
            utype = PlayerClientListener.UpdateType.City;
            break;

        case SHIPS:
            staticHandlePLAYERELEMENT_numPieces( pl, action, SOCPlayingPiece.SHIP, amount);
            utype = PlayerClientListener.UpdateType.Ship;
            break;

        case NUMKNIGHTS:
            // PLAYERELEMENT(NUMKNIGHTS) is sent after a Soldier card is played.
            {
                final SOCPlayer oldLargestArmyPlayer = ga.getPlayerWithLargestArmy();
                staticHandlePLAYERELEMENT_numKnights( ga, pl, action, amount);
                utype = PlayerClientListener.UpdateType.Knight;

                // Check for change in largest-army player; update handpanels'
                // LARGESTARMY and VICTORYPOINTS counters if so, and
                // announce with text message.
                pcl.largestArmyRefresh(oldLargestArmyPlayer, ga.getPlayerWithLargestArmy());
            }

        break;

        case CLAY:
            staticHandlePLAYERELEMENT_numRsrc( pl, action, SOCResourceConstants.CLAY, amount);
            utype = PlayerClientListener.UpdateType.Clay;
            break;

        case ORE:
            staticHandlePLAYERELEMENT_numRsrc( pl, action, SOCResourceConstants.ORE, amount);
            utype = PlayerClientListener.UpdateType.Ore;
            break;

        case SHEEP:
            staticHandlePLAYERELEMENT_numRsrc( pl, action, SOCResourceConstants.SHEEP, amount);
            utype = PlayerClientListener.UpdateType.Sheep;
            break;

        case WHEAT:
            staticHandlePLAYERELEMENT_numRsrc( pl, action, SOCResourceConstants.WHEAT, amount);
            utype = PlayerClientListener.UpdateType.Wheat;
            break;

        case WOOD:
            staticHandlePLAYERELEMENT_numRsrc( pl, action, SOCResourceConstants.WOOD, amount);
            utype = PlayerClientListener.UpdateType.Wood;
            break;

        case UNKNOWN_RESOURCE:
            /**
             * Note: if losing unknown resources, we first
             * convert player's known resources to unknown resources,
             * then remove mes's unknown resources from player.
             */
            staticHandlePLAYERELEMENT_numRsrc( pl, action, SOCResourceConstants.UNKNOWN, amount);
            utype = PlayerClientListener.UpdateType.Unknown;
            break;

        case ASK_SPECIAL_BUILD:
            staticHandlePLAYERELEMENT_simple( ga, pl, pn, action, etype, amount, null);
            // This case is not really an element update, so route as a 'request'
            pcl.requestedSpecialBuild(pl);
            break;

        case RESOURCE_COUNT:
            if (amount != pl.getResources().getTotal())
            {
                // Update count if possible; convert known to unknown if needed.
                // For our own player, server sends resource specifics, not just total count

                boolean isClientPlayer = pl.getName().equals(getClient().getNickname());
                if (! isClientPlayer)
                {
                    staticHandlePLAYERELEMENT_simple( ga, pl, pn, action, etype, amount, null);
                    pcl.playerResourcesUpdated(pl);
                }
            }
            break;

        case NUM_UNDOS_REMAINING:
            staticHandlePLAYERELEMENT_simple( ga, pl, pn, action, etype, amount, null);
            utype = PlayerClientListener.UpdateType.UndosRemaining;
            break;

        case NUM_PICK_GOLD_HEX_RESOURCES:
            staticHandlePLAYERELEMENT_simple( ga, pl, pn, action, etype, amount, null);
            pcl.requestedGoldResourceCountUpdated(pl, amount);
            break;

        case SCENARIO_SVP:
            pl.setSpecialVP(amount);
            utype = PlayerClientListener.UpdateType.SpecialVictoryPoints;
            break;

        case SCENARIO_CLOTH_COUNT:
            if (pn != -1)
            {
                pl.setCloth(amount);
            } 
            else 
            {
                ((SOCBoardLarge) (ga.getBoard())).setCloth(amount);
            }
            utype = PlayerClientListener.UpdateType.Cloth;
            break;

        case SCENARIO_WARSHIP_COUNT:
            staticHandlePLAYERELEMENT_simple( ga, pl, pn, action, etype, amount, null);
            utype = PlayerClientListener.UpdateType.Warship;
            break;

        default:
            staticHandlePLAYERELEMENT_simple( ga, pl, pn, action, etype, amount, null);
        }

        if ((pcl != null) && (utype != null))
        {
            if (! isNews)
                pcl.playerElementUpdated(pl, utype, false, false);
            else if (action == SOCPlayerElement.GAIN)
                pcl.playerElementUpdated(pl, utype, true, false);
            else
                pcl.playerElementUpdated(pl, utype, false, true);
        }
    }
    
    /** end handlePLAYERELEMENT **/
    
    /**
     * handle the "player information" message: Finds game and its {@link PlayerClientListener} by name
     * and calls {@link #handlePLAYERELEMENT(PlayerClientListener, SOCGame, SOCPlayer, int, int, PEType, int, boolean)}
     * @param mes  the message
     * @since 2.0.00
     */
    @Override
    protected void handlePLAYERELEMENTS(SOCPlayerElements mes)
    {
        final SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;
        
        final PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        final int pn = mes.getPlayerNumber();
        final SOCPlayer pl = (pn != -1) ? ga.getPlayer(pn) : null;
        final int action = mes.getAction();
        final int[] etypes = mes.getElementTypes(), amounts = mes.getAmounts();
        
        for (int i = 0; i < etypes.length; ++i)
        {
            handlePLAYERELEMENT( pcl, ga, pl, pn, action, PEType.valueOf( etypes[i] ), amounts[i], false );
        }
        if ((action == SOCPlayerElement.SET) && (etypes.length == 5) && (etypes[0] == SOCResourceConstants.CLAY)
                && (pl != null) && (ga.getGameState() == SOCGame.ROLL_OR_CARD))
            // dice roll results: when sent all known resources, clear UNKNOWN to 0
            pl.getResources().setAmount(0, SOCResourceConstants.UNKNOWN);
    }
    
    /** start handleGAMEELEMENTS  **/
    
    /**
     * Handle the GameElements message: Finds game by name, and loops calling
     * {@link #handleGAMEELEMENT( SOCGame, GEType, int )}.
     * @param mes  the message
     * @since 2.0.00
     * Identical to super class, can be omitted
     */
    protected void handleGAMEELEMENTS(final SOCGameElements mes)
    {
        final SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        final int[] etypes = mes.getElementTypes(), values = mes.getValues();
        for (int i = 0; i < etypes.length; ++i)
            handleGAMEELEMENT( ga, SOCGameElements.GEType.valueOf(etypes[i]), values[i] );
    }

     /**
     * handle "resource count" message
     * @param mes  the message
     */
    @Override
    protected void handleRESOURCECOUNT(SOCResourceCount mes)
    {
        final SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        handlePLAYERELEMENT( getClient().getClientListener(mes.getGameName()), ga, null, mes.getPlayerNumber(),
             SOCPlayerElement.SET, PEType.RESOURCE_COUNT, mes.getCount(), false);
    }

    /**
     * handle the "dice result" message
     * @param mes  the message
     */
    @Override
    protected void handleDICERESULT(SOCDiceResult mes)
    {
        final String gameName = mes.getGameName();
        SOCGame ga = getClient().getGame(gameName);
        if (ga == null)
            return;

        final int cpn = ga.getCurrentPlayerNumber();
        SOCPlayer p = null;
        if (cpn >= 0)
            p = ga.getPlayer(cpn);

        final int roll = mes.getResult();
        final SOCPlayer player = p;

        // update game state
        ga.setCurrentDice(roll);

        // notify listener
        PlayerClientListener listener = getClient().getClientListener(gameName);
        listener.diceRolled(player, roll);
    }

    /**
     * handle the "put piece" message
     * @param mes  the message
     */
    protected void handlePUTPIECE(SOCPutPiece mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        final SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
        final int coord = mes.getCoordinates();
        final int ptype = mes.getPieceType();

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;
        pcl.playerPiecePlaced(player, coord, ptype);
    }

    /**
     * handle the rare "cancel build request" message; usually not sent from
     * server to client.
     *<P>
     * - When sent from client to server, CANCELBUILDREQUEST means the player has changed
     *   their mind about spending resources to build a piece.  Only allowed during normal
     *   game play (PLACING_ROAD, PLACING_SETTLEMENT, or PLACING_CITY).
     *<P>
     *  When sent from server to client:
     *<P>
     * - During game startup (START1B or START2B): <BR>
     *       Sent from server, CANCELBUILDREQUEST means the current player
     *       wants to undo the placement of their initial settlement.
     *<P>
     * - During piece placement (PLACING_ROAD, PLACING_CITY, PLACING_SETTLEMENT,
     *                           PLACING_FREE_ROAD1 or PLACING_FREE_ROAD2):
     *<P>
     *      Sent from server, CANCELBUILDREQUEST means the player has sent
     *      an illegal PUTPIECE (bad building location). Humans can probably
     *      decide a better place to put their road, but robots must cancel
     *      the build request and decide on a new plan.
     *<P>
     *      Our client can ignore this case, because the server also sends a text
     *      message that the human player is capable of reading and acting on.
     *      For convenience, during initial placement
     *      {@link PlayerClientListener#buildRequestCanceled(SOCPlayer)}
     *      is called to reset things like {@link SOCBoardPanel} hovering pieces.
     *
     * @param mes  the message
     * @since 1.1.00
     */
    @Override
    protected void handleCANCELBUILDREQUEST(SOCCancelBuildRequest mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        final int ptype = mes.getPieceType();
        final SOCPlayer pl = ga.getPlayer(ga.getCurrentPlayerNumber());

        if (ptype >= SOCPlayingPiece.SETTLEMENT)
        {
            final int sta = ga.getGameState();
            if ((sta != SOCGame.START1B) && (sta != SOCGame.START2B) && (sta != SOCGame.START3B))
            {
                // The human player gets a text message from the server informing
                // about the bad piece placement.  So, we can ignore this message type.
                return;
            }

            if (ptype == SOCPlayingPiece.SETTLEMENT)
            {
                SOCSettlement pp = new SOCSettlement(pl, pl.getLastSettlementCoord(), null);
                ga.undoPutInitSettlement(pp);
            }
        } else {
            // ptype is -3 (SOCCancelBuildRequest.INV_ITEM_PLACE_CANCEL)
        }

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.buildRequestCanceled(pl);
    }

    /**
     * A player has discarded resources. Update player data and announce the discard.
     * @param mes  the message
     * @since 2.5.00
     */
    protected void handleDISCARD(final SOCDiscard mes)
    {
        final String gaName = mes.getGameName();
        final SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;

        final SOCPlayer pl = staticHandleDISCARD(mes, ga);
        if (pl == null)
            return;

        final PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;
        pcl.playerDiscarded(pl, mes.getResources());
    }

    /**
     * handle the "robber moved" or "pirate moved" message.
     * @param mes  the message
     */
    @Override
    protected void handleMOVEROBBER(SOCMoveRobber mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        /**
         * Note: Don't call ga.moveRobber() because that will call the
         * functions to do the stealing.  We just want to say where
         * the robber moved without seeing if something was stolen.
         */
        ga.setPlacingRobberForKnightCard(false);
        int newHex = mes.getCoordinates();
        boolean isPirate = (newHex <= 0);
        if (! isPirate)
        {
            ga.getBoard().setRobberHex(newHex, true);
        } else {
            newHex = -newHex;
            ((SOCBoardLarge) ga.getBoard()).setPirateHex(newHex, true);
        }

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.robberMoved(newHex, isPirate);
    }

    /**
     * handle the "discard request" message
     * @param mes  the message
     */
    protected void handleDISCARDREQUEST(SOCDiscardRequest mes)
    {
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.requestedDiscard(mes.getNumberOfDiscards());
    }

    /**
     * handle the "choose player request" message
     * @param mes  the message
     */
    protected void handleCHOOSEPLAYERREQUEST(SOCChoosePlayerRequest mes)
    {
        SOCGame game = getClient().getGame(mes.getGameName());
        final int maxPl = game.maxPlayers;
        final boolean[] ch = mes.getChoices();

        List<SOCPlayer> choices = new ArrayList<SOCPlayer>();
        for (int i = 0; i < maxPl; i++)
        {
            if (ch[i])
            {
                SOCPlayer p = game.getPlayer(i);
                choices.add(p);
            }
        }

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.requestedChoosePlayer(choices, mes.canChooseNone());
    }

    /**
     * The server wants this player to choose to rob cloth or rob resources,
     * after moving the pirate ship.  Added 2012-11-17 for v2.0.00.
     */
    protected void handleCHOOSEPLAYER(SOCChoosePlayer mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        int victimPlayerNumber = mes.getChoice();
        SOCPlayer player = ga.getPlayer(victimPlayerNumber);

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.requestedChooseRobResourceType(player);
    }

     /**
     * handle the "make offer" message
     * @param mes  the message
     */
    protected void handleMAKEOFFER(final SOCMakeOffer mes)
    {
        final String gaName = mes.getGameName();
        final SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;

        SOCTradeOffer offer = mes.getOffer();
        final int fromPN = offer.getFrom();
        SOCPlayer from = ga.getPlayer(fromPN);
        from.setCurrentOffer(offer);

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        pcl.requestedTrade(from, fromPN);
    }

    /**
     * handle the "clear offer" message
     * @param mes  the message
     */
    @Override
    protected void handleCLEAROFFER(SOCClearOffer mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        final int pn = mes.getPlayerNumber();
        SOCPlayer player = null;
        if (pn != -1)
        {
            player = ga.getPlayer(pn);
            player.setCurrentOffer(null);
        } else {
            for (int i = 0; i < ga.maxPlayers; ++i)
                ga.getPlayer(i).setCurrentOffer(null);
        }

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.requestedTradeClear(player, false);
    }

    /**
     * handle the "reject offer"/"disallowed trade" message:
     * a player has rejected an offer,
     * or server has disallowed our trade-related request.
     * @param mes  the message
     */
    protected void handleREJECTOFFER(SOCRejectOffer mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        final int pn = mes.getPlayerNumber();
        SOCPlayer player = (pn >= 0) ? ga.getPlayer(pn) : null;

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());

        int rc = mes.getReasonCode();
        switch (rc)
        {
        case 0:
            pcl.requestedTradeRejection(player);
            break;

        case SOCRejectOffer.REASON_NOT_YOUR_TURN:
            pcl.playerTradeDisallowed(-1, false, true);
            break;

        case SOCRejectOffer.REASON_CANNOT_MAKE_OFFER:
            pcl.playerTradeDisallowed(pn, true, false);
            break;

        default:
            pcl.playerTradeDisallowed(pn, false, false);
        }
    }

    /**
     * handle the "accept offer" message
     * @param mes  the message
     * @since 2.0.00
     */
    protected void handleACCEPTOFFER(final SOCAcceptOffer mes)
    {
        final String gaName = mes.getGameName();
        final SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;

        staticHandleACCEPTOFFER(mes, ga);

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;

        pcl.playerTradeAccepted
            (ga.getPlayer(mes.getOfferingNumber()), ga.getPlayer(mes.getAcceptingNumber()),
             mes.getResToOfferingPlayer(), mes.getResToAcceptingPlayer());
    }

    /**
     * handle the "clear trade message" message
     * @param mes  the message
     */
    protected void handleCLEARTRADEMSG(SOCClearTradeMsg mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        int pn = mes.getPlayerNumber();
        SOCPlayer player = null;
        if (pn != -1)
            player = ga.getPlayer(pn);

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        pcl.requestedTradeReset(player);
    }
    
    /**
     * handle the "bank trade" message from a v2.0.00 or newer server.
     * Calls {@link MessageHandler#handleBANKTRADE(SOCBankTrade, SOCGame)}
     * if server is v2.5.00 or newer ({@link SOCBankTrade#VERSION_FOR_OMIT_PLAYERELEMENTS}).
     *
     * @param mes  the message
     * @ param isPractice  Is the server in process, not remote?
     * @since 2.0.00
     */
    protected void handleBANKTRADE(final SOCBankTrade mes /* , final boolean isPractice */)
    {
        final String gaName = mes.getGameName();
        final SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;
        
        if ((getClient().sVersion >= SOCBankTrade.VERSION_FOR_OMIT_PLAYERELEMENTS))
            staticHandleBANKTRADE(mes, ga);
        
        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;
        
        pcl.playerBankTrade(ga.getPlayer(mes.getPlayerNumber()), mes.getGiveSet(), mes.getGetSet());
    }
    
    /**
     * Handle the "development card action" message, which may have multiple cards.
     * Updates game data by calling {@link #handleDEVCARDACTION(SOCGame, SOCPlayer, boolean, int, int)},
     * then calls {@link PlayerClientListener#playerDevCardsUpdated(SOCPlayer, boolean)}.
     *<P>
     * If message is about the player's own cards at end of game when server reveals all VP cards
     * ({@link SOCDevCardAction#ADD_OLD} in state {@link SOCGame#OVER}),
     * returns immediately and doesn't call those methods.
     *
     * @param mes  the message
     */
    @Override
    protected void handleDEVCARDACTION( /* final boolean isPractice, */ final SOCDevCardAction mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        final int pn = mes.getPlayerNumber();
        final SOCPlayer player = ga.getPlayer(pn);
        final PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        final boolean isClientPlayer = (pcl != null) && (pn >= 0) && (pn == pcl.getClientPlayerNumber());
        final int act = mes.getAction();

        if (isClientPlayer && (act == SOCDevCardAction.ADD_OLD) && (ga.getGameState() == SOCGame.OVER))
        {
            return;
        }

        final List<Integer> ctypes = mes.getCardTypes();
        if (ctypes != null)
        {
            for (final int ctype : ctypes)
                handleDEVCARDACTION(ga, player, isClientPlayer, act, ctype);
        } else {
            int ctype = mes.getCardType();
            if ( /* (! isPractice) && */ (getClient().sVersion < SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES))
            {
                if (ctype == SOCDevCardConstants.KNIGHT_FOR_VERS_1_X)
                    ctype = SOCDevCardConstants.KNIGHT;
                else if (ctype == SOCDevCardConstants.UNKNOWN_FOR_VERS_1_X)
                    ctype = SOCDevCardConstants.UNKNOWN;
            }
            handleDEVCARDACTION(ga, player, isClientPlayer, act, ctype);
        }

        if (pcl != null)
            pcl.playerDevCardsUpdated(player, (act == SOCDevCardAction.ADD_OLD));
    }

    /**
     * Handle one dev card's game data update for {@link #handleDEVCARDACTION(SOCDevCardAction)}.
     * In case this is part of a list of cards, does not call
     * {@link PlayerClientListener#playerDevCardsUpdated(SOCPlayer, boolean)}: Caller must do so afterwards.
     * For {@link SOCDevCardAction#PLAY}, calls {@link SOCPlayer#updateDevCardsPlayed(int, boolean)}.
     *
     * @param ga  Game being updated
     * @param player  Player in {@code ga} being updated
     * @param isClientPlayer  True if {@code player} is our client playing in {@code ga}
     * @param act  Action being done: {@link SOCDevCardAction#DRAW}, {@link SOCDevCardAction#PLAY PLAY},
     *     {@link SOCDevCardAction#ADD_OLD ADD_OLD}, or {@link SOCDevCardAction#ADD_NEW ADD_NEW}
     * @param ctype  Type of development card from {@link SOCDevCardConstants}
     * @see MessageHandler#handleDEVCARDACTION(SOCGame, SOCPlayer, int, int)
     */
    protected void handleDEVCARDACTION
        (final SOCGame ga, final SOCPlayer player, final boolean isClientPlayer, final int act, final int ctype)
    {
        /** if you change this method, consider changing {@link MessageHandler#handleDEVCARDACTION}
            and {@link SOCRobotBrain#handleDEVCARDACTION} too **/

        switch (act)
        {
        case SOCDevCardAction.DRAW:
            player.getInventory().addDevCard(1, SOCInventory.NEW, ctype);
            break;

        case SOCDevCardAction.PLAY:
            player.getInventory().removeDevCard(SOCInventory.OLD, ctype);
            player.updateDevCardsPlayed(ctype, false);
            if ((ctype == SOCDevCardConstants.KNIGHT) && ! ga.isGameOptionSet(SOCGameOptionSet.K_SC_PIRI))
                ga.setPlacingRobberForKnightCard(true);
            break;

        case SOCDevCardAction.ADD_OLD:
            player.getInventory().addDevCard(1, SOCInventory.OLD, ctype);
            break;

        case SOCDevCardAction.ADD_NEW:
            player.getInventory().addDevCard(1, SOCInventory.NEW, ctype);
            break;
        }
    }

    /**
     * handle the "set played dev card flag" message
     * @param mes  the message
     */
    protected void handleSETPLAYEDDEVCARD(SOCSetPlayedDevCard mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        staticHandlePLAYERELEMENT_simple( ga, null, mes.getPlayerNumber(), SOCPlayerElement.SET,
             PEType.PLAYED_DEV_CARD_FLAG, mes.hasPlayedDevCard() ? 1 : 0, null);
    }

    /**
     * handle the "list of potential settlements" message
     * @param mes  the message
     * @throws IllegalStateException if the board has
     *     {@link SOCBoardLarge#getAddedLayoutPart(String) SOCBoardLarge.getAddedLayoutPart("AL")} != {@code null} but
     *     badly formed (node list number 0, or a node list number not followed by a land area number).
     *     This Added Layout Part is rarely used, and that would be discovered quickly while testing
     *     the board layout that contained it (see TestBoardLayout.testSingleLayout).
     */
    protected void handlePOTENTIALSETTLEMENTS(SOCPotentialSettlements mes)
            throws IllegalStateException
    {
        final String gaName = mes.getGameName();
        staticHandlePOTENTIALSETTLEMENTS(mes, getClient().getGame(gaName));

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl != null)
            pcl.boardPotentialsUpdated();
    }

    /**
     * handle the "change face" message
     * @param mes  the message
     */
    @Override
    protected void handleCHANGEFACE(SOCChangeFace mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        player.setFaceId(mes.getFaceId());
        pcl.playerFaceChanged(player, mes.getFaceId());
    }

    /**
     * handle the "reject socketConnection" message
     * @param mes  the message
     */
    @Override
    protected void handleREJECTCONNECTION(SOCRejectConnection mes)
    {
        // TODO: this needs a lot of work, because a client may have two connections.
//        getClient().socketConnection.disconnect();

        String txt = mes.getText();
        if (getClient().sVersion > 0)
        {
            // "Disconnected by server version {0}:"
            txt = getClient().strings.get( "pcli.error.server.disconnected_by_version",
                    Version.version( getClient().sVersion ) ) + '\n' + txt;
        }
        getClient().getMainDisplay().showErrorPanel(txt, true );
    }

    /**
     * handle the "set seat lock" message
     * @param mes  the message
     */
    protected void handleSETSEATLOCK(SOCSetSeatLock mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;

        final SOCGame.SeatLockState[] sls = mes.getLockStates();
        if (sls == null)
            ga.setSeatLock(mes.getPlayerNumber(), mes.getLockState());
        else
            ga.setSeatLocks(sls);

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        pcl.seatLockUpdated();
    }

    /**
     * handle the "roll dice prompt" message;
     *   if we're in a game and we're the dice roller,
     *   either set the auto-roll timer, or prompt to roll or choose card.
     *
     * @param mes  the message
     * @since 1.1.00
     */
    protected void handleROLLDICEPROMPT(SOCRollDicePrompt mes)
    {
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;  // Not one of our games

        pcl.requestedDiceRoll(mes.getPlayerNumber());
    }

    /**
     * handle board reset
     * (new game with same players, same game name, new layout).
     * Create new Game object, destroy old one.
     * For human players, the reset message will be followed
     * with others which will fill in the game state.
     * For robots, they must discard game state and ask to re-join.
     *
     * @param mes  the message
     *
     * @see soc.server.SOCServer#resetBoardAndNotify(String, int)
     * @see soc.game.SOCGame#resetAsCopy()
     * @since 1.1.00
     */
    @Override
    protected void handleRESETBOARDAUTH(SOCResetBoardAuth mes)
    {
        String gname = mes.getGameName();
        SOCGame ga = getClient().getGame(gname);
        if (ga == null)
            return;  // Not one of our games
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;  // Not one of our games

        SOCGame greset = ga.resetAsCopy();
        greset.isPractice = ga.isPractice;
        getClient().addGame( greset );
        pcl.boardReset(greset, mes.getRejoinPlayerNumber(), mes.getRequestingPlayerNumber());
        ga.destroyGame();
    }

    /**
     * a player is requesting a board reset: we must update
     * local game state, and vote unless we are the requester.
     *
     * @param mes  the message
     * @since 1.1.00
     */
    protected void handleRESETBOARDVOTEREQUEST(SOCResetBoardVoteRequest mes)
    {
        String gname = mes.getGameName();
        SOCGame ga = getClient().getGame(gname);
        if (ga == null)
            return;  // Not one of our games
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;  // Not one of our games

        SOCPlayer player = ga.getPlayer(mes.getRequestingPlayer());
        pcl.boardResetVoteRequested(player);
    }

    /**
     * another player has voted on a board reset request: display the vote.
     *
     * @param mes  the message
     * @since 1.1.00
     */
    protected void handleRESETBOARDVOTE(SOCResetBoardVote mes)
    {
        String gname = mes.getGameName();
        SOCGame ga = getClient().getGame(gname);
        if (ga == null)
            return;  // Not one of our games
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;  // Not one of our games

        SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
        pcl.boardResetVoteCast(player, mes.getPlayerVote());
    }

    /**
     * voting complete, board reset request rejected
     *
     * @param mes  the message
     * @since 1.1.00
     */
    protected void handleRESETBOARDREJECT(SOCResetBoardReject mes)
    {
        String gname = mes.getGameName();
        SOCGame ga = getClient().getGame(gname);
        if (ga == null)
            return;  // Not one of our games
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;  // Not one of our games

        pcl.boardResetVoteRejected();
    }

    /**
     * process the "game option get defaults" message.
     * If any default option's keyname is unknown, ask the server.
     * @see ServerGametypeInfo
     * @since 1.1.07
     */
    protected void handleGAMEOPTIONGETDEFAULTS(SOCGameOptionGetDefaults mes )
    {
        ServerGametypeInfo servOpts = getClient().tcpServGameOpts;

        final List<String> unknowns;
        synchronized(servOpts)
        {
            // receiveDefaults sets opts.defaultsReceived, may set opts.allOptionsReceived
            unknowns = servOpts.receiveDefaults( SOCGameOption.parseOptionsToMap(mes.getOpts(), servOpts.knownOpts));
        }

        if (unknowns != null)
        {
            getClient().getMainDisplay().optionsRequested();
            getClient().sendMessage( new SOCGameOptionGetInfos(unknowns, getClient().wantsI18nStrings(false ), false).toCmd());
        }
        else
        {
            servOpts.newGameWaitingForOpts = false;
            getClient().getMainDisplay().optionsReceived(servOpts, false );
        }
    }

    /**
     * process the "game option info" message
     * by calling {@link ServerGametypeInfo#receiveInfo(SOCGameOptionInfo)}.
     * If all are now received, possibly show game info/options window for new game or existing game.
     *<P>
     * For a summary of the flags and variables involved with game options,
     * and the client/server interaction about their values, see
     * {@link ServerGametypeInfo}.
     *<P>
     * When first connected to a server having a different version, the client negotiates available options.
     * To avoid hanging on this process because of a very slow network or bug,
     * {@link SwingMainDisplay.GameOptionsTimeoutTask} can eventually call this
     * method to signal that all options have been received.
     *
     * @since 1.1.07
     */
    @Override
    protected void handleGAMEOPTIONINFO(SOCGameOptionInfo mes /*, final boolean isPractice */ )
    {
        ServerGametypeInfo opts = getClient().tcpServGameOpts;

        boolean hasAllNow;
        synchronized(opts)
        {
            hasAllNow = opts.receiveInfo(mes);
        }

        boolean isDash = mes.getOptionNameKey().equals("-");  // I18N OK: do not localize "-" or any other keyname
        getClient().getMainDisplay().optionsReceived(opts, false, isDash, hasAllNow);
    }

    /**
     * process the "new game with options" message
     * @since 1.1.07
     */
    protected void handleNEWGAMEWITHOPTIONS(final SOCNewGameWithOptions mes /*, final boolean isPractice */)
    {
        // Note: Must run in network message thread, not AWT event thread,
        // in case client is about to be auth'd to join this game:
        // messages must take effect in the order sent

        String gname = mes.getGameName();
        final String opts = mes.getOptionsString();

        boolean canJoin = (mes.getMinVersion() <= Version.versionNumber());
        if (gname.charAt(0) == SOCGames.MARKER_THIS_GAME_UNJOINABLE)
        {
            gname = gname.substring(1);
            canJoin = false;
        }

        getClient().getMainDisplay().addToGameList /* display */ (! canJoin, gname, opts, true /* ! isPractice */);
    }

    /**
     * handle the "list of games with options" message
     * @since 1.1.07
     */
    protected void handleGAMESWITHOPTIONS(SOCGamesWithOptions mes )
    {
        // Any game's name in this msg may start with the "unjoinable" prefix
        // SOCGames.MARKER_THIS_GAME_UNJOINABLE.
        // This is recognized and removed in mes.getGameList.
        final SOCGameList msgGames = mes.getGameList( getClient().tcpServGameOpts.knownOpts );
        if (msgGames == null)
            return;

           // practice gameoption data is set up in handleVERSION;
           // practice srv's gamelist is reached through practiceServer obj.
        if (getClient().serverGames == null)
            getClient().serverGames = msgGames;
        else
            getClient().serverGames.addGames(msgGames, Version.versionNumber());

        // No more game-option info will be received,
        // because that's always sent before game names are sent.
        // We may still ask for GAMEOPTIONGETDEFAULTS if asking to create a game,
        // but that will happen when user clicks that button, not yet.
        getClient().tcpServGameOpts.noMoreOptions(false);
        

        // update displayed list on AWT event thread, not network message thread,
        // to ensure right timing for repaint to avoid appearing empty.
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                final MainDisplay mdisp = getClient().getMainDisplay();

                for (String gaName : msgGames.getGameNames())
                    mdisp.addToGameList( msgGames.isUnjoinableGame(gaName), gaName, msgGames.getGameOptionsString(gaName), true );

                mdisp.repaintGameAndChannelLists();
            }
        });
    }

    /**
     * Localized i18n strings for game items.
     * Added 2015-01-11 for v2.0.00.
     */
    protected void handleLOCALIZEDSTRINGS(final SOCLocalizedStrings mes /*, final boolean isPractice */)
    {
        final SOCGameOptionSet knownOpts = getClient().tcpServGameOpts.knownOpts;

        final List<String> strs = mes.getParams();
        final String type = strs.get(0);

        if (type.equals(SOCLocalizedStrings.TYPE_GAMEOPT))
        {
            final int L = strs.size();
            for (int i = 1; i < L; i += 2)
            {
                SOCGameOption opt = knownOpts.getKnownOption(strs.get(i), false);
                if (opt != null)
                {
                    final String desc = strs.get(i + 1);
                    if ((desc != null) && (!desc.isEmpty()))
                        opt.setDesc(desc);
                }
            }
        }
        else if (type.equals(SOCLocalizedStrings.TYPE_SCENARIO))
        {
//           getClient().localizeGameScenarios( strs, true, mes.isFlagSet(SOCLocalizedStrings.FLAG_SENT_ALL), false );
        }
        else
        {
            System.err.println("L4916: Unknown localized string type " + type);
        }
    }

    /**
     * Updated scenario info.
     * Added 2015-09-21 for v2.0.00.
     * @ param isPractice  Is the server in process, not remote?
     */
    protected void handleSCENARIOINFO(final SOCScenarioInfo mes /*, final boolean isPractice */)
    {
        ServerGametypeInfo opts = getClient().tcpServGameOpts;
        updateScenarioInfo( mes, opts );
    }
    
    protected void updateScenarioInfo( SOCScenarioInfo mes,  ServerGametypeInfo opts )
    {
        if (mes.noMoreScens)
        {
            synchronized (opts)
            {
                opts.allScenStringsReceived = true;
                opts.allScenInfoReceived = true;
            }
        } else {
            final String scKey = mes.getScenarioKey();

            if (mes.isKeyUnknown)
                SOCScenario.removeUnknownScenario(scKey);
            else
                SOCScenario.addKnownScenario(mes.getScenario());

            synchronized (opts)
            {
                opts.scenKeys.add(scKey);  // OK if was already present from received localized strings
            }
        }
    }

    /**
     * handle the "player stats" message
     * @since 1.1.09
     */
    private void handlePLAYERSTATS(SOCPlayerStats mes)
    {
        final String gaName = mes.getGameName();
        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;  // Not one of our games
        final int clientPN = pcl.getClientPlayerNumber();
        if (clientPN == -1)
            return;  // Not one of our games

        staticHandlePLAYERSTATS(mes, pcl.getGame(), clientPN);  // update player data

        final int stype = mes.getStatType();
        switch (stype)
        {
        case SOCPlayerStats.STYPE_RES_ROLL:
            // fallthrough
        case SOCPlayerStats.STYPE_TRADES:
            pcl.playerStats(stype, mes.getParams(), true, true);
            break;
        default:
            System.err.println("handlePLAYERSTATS: unrecognized player stat type " + stype);
        }
    }

    /**
     * Handle the server's debug piece placement on/off message.
     * @since 1.1.12
     */
    private void handleDEBUGFREEPLACE(SOCDebugFreePlace mes)
    {
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;  // Not one of our games

        pcl.debugFreePlaceModeToggled(mes.getCoordinates() == 1);
    }

    /**
     * Handle server responses from the "simple request" handler.
     * @since 1.1.18
     */
    private void handleSIMPLEREQUEST(SOCSimpleRequest mes)
    {
        final String gaName = mes.getGameName();

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;  // Not one of our games

        handleSIMPLEREQUEST(mes, getClient().getGame(gaName));  // update any game data
        pcl.simpleRequest(mes.getPlayerNumber(), mes.getRequestType(), mes.getValue1(), mes.getValue2());
    }
    
    /**
     * Update any game data from "simple request" announcements from the server.
     * Currently, ignores them except for:
     *<UL>
     * <LI> {@link SOCSimpleRequest#TRADE_PORT_PLACE TRADE_PORT_PLACE}:
     *     Calls {@link SOCGame#placePort(SOCPlayer, int, int)} if {@code pn} &gt;= 0
     *</UL>
     *
     * @param mes  the message
     * @param ga  Game the client is playing,
     *     for method reuse by SOCPlayerClient; does nothing if {@code null}
     * @since 2.0.00
     */
    public static void handleSIMPLEREQUEST(final SOCSimpleRequest mes, final SOCGame ga)
    {
        if (ga == null)
            return;  // Not one of our games
        
        final int pn = mes.getPlayerNumber(),
                rtype = mes.getRequestType(),
                value1 = mes.getValue1(),
                value2 = mes.getValue2();
        
        switch (rtype)
        {
        // Types which may update some game data:
        
        case SOCSimpleRequest.TRADE_PORT_PLACE:
            if (pn >= 0)  // if pn -1, request was rejected
                ga.placePort(ga.getPlayer(pn), value1, value2);
            break;
        
        // Known types with no game data update:
        // Catch these before default case, so 'unknown type' won't be printed
        
        case SOCSimpleRequest.PROMPT_PICK_RESOURCES:
        case SOCSimpleRequest.SC_PIRI_FORT_ATTACK:
            break;
        
        default:
            // Ignore unknown types.
            // Since the bots and server are almost always the same version, this
            // shouldn't often occur: print for debugging.
            System.err.println
                    ("DPC.handleSIMPLEREQUEST: Unknown type ignored: " + rtype + " in game " + ga.getName());
        }
    }
    
    /** start handle simple action **/
    
    /**
     * Update any game data from "simple action" announcements from the server.
     * @since 1.1.19
     */
    @SuppressWarnings("fallthrough")
    private final void handleSIMPLEACTION(final SOCSimpleAction mes)
    {
        final String gaName = mes.getGameName();
        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;  // Not one of our games

        final int atype = mes.getActionType();
        switch (atype)
        {
        case SOCSimpleAction.SC_PIRI_FORT_ATTACK_RESULT:
            // present the server's response to a Pirate Fortress Attack request
            pcl.scen_SC_PIRI_pirateFortressAttackResult(false, mes.getValue1(), mes.getValue2());
            break;

        case SOCSimpleAction.BOARD_EDGE_SET_SPECIAL:
            // fall through: displayless sets game data, pcl.simpleAction displays updated board layout

        case SOCSimpleAction.TRADE_PORT_REMOVED:
        case SOCSimpleAction.DEVCARD_BOUGHT:
            staticHandleSIMPLEACTION( mes, getClient().getGame(gaName) );
            // fall through so pcl.simpleAction updates displayed board and game data

        case SOCSimpleAction.RSRC_TYPE_MONOPOLIZED:
            pcl.simpleAction(mes.getPlayerNumber(), atype, mes.getValue1(), mes.getValue2());
            break;

        default:
            // ignore unknown types
            {
                final int mesPN = mes.getPlayerNumber();
                if ((mesPN >= 0) && (mesPN == pcl.getClientPlayerNumber()))
                    System.err.println
                        ("handleSIMPLEACTION: Unknown type ignored: " + atype + " in game " + gaName);
            }
        }
    }

    /**
     * Handle game server text and announcements.
     * @see #handleGAMETEXTMSG(SOCGameTextMsg)
     * @since 2.0.00
     */
    protected void handleGAMESERVERTEXT(SOCGameServerText mes)
    {
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;

        pcl.messageReceived(null, mes.getText());
    }
    
    /** start handleDICERESULTRESOURCES */

    /**
     * Handle all players' dice roll result resources.  Looks up the game,
     * players gain resources, and announces results.
     * @since 2.0.00
     */
    @Override
    protected void handleDICERESULTRESOURCES(final SOCDiceResultResources mes)
    {
        SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;
        
        // Call static method
        staticHandleDICERESULTRESOURCES(mes, ga, null, true);
        pcl.diceRolledResources(mes.playerNum, mes.playerRsrc);

        // handle total counts here, visually updating any discrepancies
        final int n = mes.playerNum.size();
        for (int i = 0; i < n; ++i)
            handlePLAYERELEMENT
                (getClient().getClientListener(mes.getGameName()), ga, null, mes.playerNum.get(i),
                 SOCPlayerElement.SET, PEType.RESOURCE_COUNT, mes.playerResTotal.get(i), false);
    }

    /**
     * Handle moving a piece (a ship) around on the board.
     * @since 2.0.00
     */
    @Override
    protected void handleMOVEPIECE(SOCMovePiece mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;  // Not one of our games

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;
        SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
        pcl.playerPieceMoved(player, mes.getFromCoord(), mes.getToCoord(), mes.getPieceType());
    }

    /**
     * Handle removing a piece (a ship) from the board in certain scenarios.
     * @since 2.0.00
     */
    @Override
    protected void handleREMOVEPIECE(SOCRemovePiece mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;  // Not one of our games

        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;
        SOCPlayer player = ga.getPlayer(mes.getParam1());
        pcl.playerPieceRemoved(player, mes.getParam3(), mes.getParam2());
    }

    /**
     * Handle an undo put piece / move piece.
     * @since 2.7.00
     */
    private void handleUNDOPUTPIECE(final SOCUndoPutPiece mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;  // Not one of our games
        
        // Call static method
        staticHandleUNDOPUTPIECE(mes, ga);

        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;  // Not one of our games
        final int pn = mes.getPlayerNumber(),
            pieceType = mes.getPieceType(),
            fromCoord = mes.getMovedFromCoordinates();
        if (pn >= 0)
            pcl.playerPiecePlacementUndone(ga.getPlayer(pn), mes.getCoordinates(), fromCoord, pieceType);
        else
            pcl.playerPiecePlacementUndoDeclined(pieceType, (fromCoord != 0));
    }

    /**
     * Reveal a hidden hex on the board.
     * @since 2.0.00
     */
    @Override
    protected void handleREVEALFOGHEX( SOCRevealFogHex mes )
    {
        String gaName = mes.getGameName();
//        ga.revealFogHiddenHex(mes.getParam1(), mes.getParam2(), mes.getParam3());
        super.handleREVEALFOGHEX( mes );
        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;  // Not one of our games
        pcl.boardUpdated();
    }

    /**
     * Update a village piece's value on the board (cloth remaining) in _SC_CLVI,
     * or a pirate fortress's strength in _SC_PIRI.
     * @since 2.0.00
     */
    @Override
    protected void handlePIECEVALUE(final SOCPieceValue mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;  // Not one of our games
        if (! ga.hasSeaBoard)
            return;  // should not happen

        final int coord = mes.getParam2();
        final int pv = mes.getParam3();
        SOCPlayingPiece updatePiece = null;  // if not null, call pcl.pieceValueUpdated

        if (ga.isGameOptionSet(SOCGameOptionSet.K_SC_CLVI))
        {
            SOCVillage vi = ((SOCBoardLarge) (ga.getBoard())).getVillageAtNode(coord);
            if (vi != null)
            {
                vi.setCloth(pv);
                updatePiece = vi;
            }
        }
        else if (ga.isGameOptionSet(SOCGameOptionSet.K_SC_PIRI))
        {
            SOCFortress fort = ga.getFortress(coord);
            if (fort != null)
            {
                fort.setStrength(pv);
                updatePiece = fort;
            }
        }

        if (updatePiece != null)
        {
            PlayerClientListener pcl = getClient().getClientListener(gaName);
            if (pcl != null)
                pcl.pieceValueUpdated(updatePiece);
        }
    }

    /**
     * Text that a player has been awarded Special Victory Point(s).
     * The server will also send a {@link SOCPlayerElement} with the SVP total.
     * Also sent for each player's SVPs when client is joining a game in progress.
     * @since 2.0.00
     */
    protected void handleSVPTEXTMSG(final SOCSVPTextMessage mes)
    {
        final String gaName = mes.getGameName();
        SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;  // Not one of our games
        final SOCPlayer pl = ga.getPlayer(mes.pn);
        if (pl == null)
            return;

        pl.addSpecialVPInfo(mes.svp, mes.desc);
        PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;

        pcl.playerSVPAwarded(pl, mes.svp, mes.desc);
    }
    
    /** start handleINVENTORYITEMACTION **/
    
    /**
     * Update player inventory. Refresh our display. If it's a reject message, give feedback to the user.
     * @since 2.0.00
     */
    private void handleINVENTORYITEMACTION(final SOCInventoryItemAction mes)
    {
        final boolean isRejected =
                staticHandleINVENTORYITEMACTION( mes, getClient().getGame(mes.getGameName()));
 
        PlayerClientListener pcl = getClient().getClientListener( mes.getGameName() );
        if (pcl == null)
            return;

        if (isRejected)
        {
            pcl.invItemPlayRejected(mes.itemType, mes.reasonCode);
        } else {
            SOCGame ga = getClient().getGame(mes.getGameName());
            if (ga != null)
            {
                final SOCPlayer pl = ga.getPlayer(mes.playerNumber);
                pcl.playerDevCardsUpdated
                    (pl, (mes.action == SOCInventoryItemAction.ADD_PLAYABLE));
                if (mes.action == SOCInventoryItemAction.PLAYED)
                    pcl.playerCanCancelInvItemPlay(pl, mes.canCancelPlay);
            }
        }
    }

    /**
     * Handle the "set special item" message.
     * Calls {@link #handleSETSPECIALITEM(SOCSetSpecialItem)},
     * then calls {@link PlayerClientListener} to update the game display.
     *
     * @param mes  the message
     * @since 2.0.00
     */
    private void handleSETSPECIALITEM( SOCSetSpecialItem mes)
    {
        // update game data:
        staticHandleSETSPECIALITEM( mes, getClient().getGame( mes.getGameName() ));

        final PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl == null)
            return;

        final SOCGame ga = getClient().getGame(mes.getGameName());
        if (ga == null)
            return;

        // update displays:

        final String typeKey = mes.typeKey;
        final int gi = mes.gameItemIndex, pi = mes.playerItemIndex, pn = mes.playerNumber;
        final SOCPlayer pl = ((pn != -1) && (pi != -1)) ? ga.getPlayer(pn) : null;

        switch (mes.op)
        {
        case SOCSetSpecialItem.OP_SET:
            // fall through
        case SOCSetSpecialItem.OP_CLEAR:
            pcl.playerSetSpecialItem(typeKey, ga, pl, gi, pi, (mes.op == SOCSetSpecialItem.OP_SET));
            break;

        case SOCSetSpecialItem.OP_PICK:
            // fall through
        case SOCSetSpecialItem.OP_DECLINE:
            pcl.playerPickSpecialItem(typeKey, ga, pl, gi, pi, (mes.op == SOCSetSpecialItem.OP_PICK),
                mes.coord, mes.level, mes.sv);
            break;

        case SOCSetSpecialItem.OP_SET_PICK:
            // fall through
        case SOCSetSpecialItem.OP_CLEAR_PICK:
            pcl.playerSetSpecialItem(typeKey, ga, pl, gi, pi, (mes.op == SOCSetSpecialItem.OP_SET_PICK));
            pcl.playerPickSpecialItem(typeKey, ga, pl, gi, pi, true,
                mes.coord, mes.level, mes.sv);
            break;
        }
    }
    
    /**
     * Handle the "robbery result" message.
     * @param mes  the message
     * @since 2.5.00
     */
    protected void handleROBBERYRESULT(final SOCRobberyResult mes )
    {
        SOCGame ga = getClient().getGame( mes.getGameName() );
        staticHandleROBBERYRESULT(mes, ga);
        
        PlayerClientListener pcl = getClient().getClientListener(mes.getGameName());
        if (pcl != null)
            pcl.reportRobberyResult( mes.perpPN, mes.victimPN, mes.resType, mes.resSet, mes.peType,
                     mes.isGainLose, mes.amount, mes.victimAmount, mes.extraValue );
    }
    
    /**
     * Handle the "Player has Picked Resources" message by updating player resource data.
     * @param mes  the message
     * @since 2.5.00
     */
    protected void handlePICKRESOURCES( SOCPickResources mes )
    {
        SOCGame ga = getClient().getGame(((SOCMessageForGame) mes).getGameName());
        if (! staticHandlePICKRESOURCES(mes, ga))
            return;
        
        PlayerClientListener pcl = getClient().getClientListener(ga.getName());
        if (pcl != null)
            pcl.playerPickedResources
                    (ga.getPlayer(mes.getPlayerNumber()), mes.getResources(), mes.getReasonCode());
    }
    
    /**
     * Server has declined our player's request.
     * Calls {@link PlayerClientListener#playerRequestDeclined(int, int, int, String)}
     * and maybe {@link #handleGAMESTATE(SOCGame, int, boolean, boolean)}.
     * @since 2.5.00
     */
    protected void handleDECLINEPLAYERREQUEST(final SOCDeclinePlayerRequest mes)
    {
        final String gaName = mes.getGameName();
        final PlayerClientListener pcl = getClient().getClientListener(gaName);
        if (pcl == null)
            return;
        final SOCGame ga = getClient().getGame(gaName);
        if (ga == null)
            return;

        pcl.playerRequestDeclined(mes.reasonCode, mes.detailValue1, mes.detailValue2, mes.reasonText);
        final int currState = mes.gameState;
        if (currState != 0)
            handleGAMESTATE(ga, currState, false, true);
    }

}  // class PlayerMessageHandler