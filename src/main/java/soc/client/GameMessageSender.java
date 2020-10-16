/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2019-2020 Jeremy D Monin <jeremy@nand.net>
 * Extracted in 2019 from SOCPlayerClient.java, so:
 * Portions of this file Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2019 Jeremy D Monin <jeremy@nand.net>
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

import java.util.Map;

import soc.communication.Connection;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCSpecialItem;
import soc.game.SOCTradeOffer;
import soc.message.SOCAcceptOffer;
import soc.message.SOCBankTrade;
import soc.message.SOCBuildRequest;
import soc.message.SOCBuyDevCardRequest;
import soc.message.SOCCancelBuildRequest;
import soc.message.SOCChangeFace;
import soc.message.SOCChoosePlayer;
import soc.message.SOCClearOffer;
import soc.message.SOCDebugFreePlace;
import soc.message.SOCDiscard;
import soc.message.SOCEndTurn;
import soc.message.SOCGameTextMsg;
import soc.message.SOCInventoryItemAction;
import soc.message.SOCLeaveGame;
import soc.message.SOCMakeOffer;
import soc.message.SOCMessage;
import soc.message.SOCMovePiece;
import soc.message.SOCMoveRobber;
import soc.message.SOCPickResourceType;
import soc.message.SOCPickResources;
import soc.message.SOCPlayDevCardRequest;
import soc.message.SOCPutPiece;
import soc.message.SOCRejectOffer;
import soc.message.SOCResetBoardRequest;
import soc.message.SOCResetBoardVote;
import soc.message.SOCRollDice;
import soc.message.SOCSetSeatLock;
import soc.message.SOCSetSpecialItem;
import soc.message.SOCSimpleRequest;
import soc.message.SOCSitDown;
import soc.message.SOCStartGame;

/**
 * Client class to form outgoing messages and call {@link ClientNetwork} methods to send them to the server.
 * In-game actions and requests each have their own methods, such as {@link #buyDevCard(SOCGame)}.
 * General messages can be sent using {@link #net.send( String, boolean)}.
 *<P>
 * Before v2.0.00, most of these fields and methods were part of the main {@link SOCPlayerClient} class.
 *
 * @author paulbilnoski
 * @since 2.0.00
 */
/*package*/ class GameMessageSender
{
    private final SOCPlayerClient client;
    private final ClientNetwork net;
    private final Map<String, PlayerClientListener> clientListeners;

    GameMessageSender(final SOCPlayerClient client, Map<String, PlayerClientListener> clientListeners)
    {
        this.client = client;
        if (client == null)
            throw new IllegalArgumentException("client is null");
        net = client.getNet();
        if (net == null)
            throw new IllegalArgumentException("client network is null");
        this.clientListeners = clientListeners;
    }

    public Connection getConnection()
    {
        return net.prCli;
    }
    /**
     * Send a message to the net or practice server by calling {@link ClientNetwork} methods.
     * This is a convenience method. Because the player can be in both network games and practice games,
     * uses {@code isPractice} to route to the appropriate client-server connection.
     *
     * @param s  the message command, formatted by a {@code soc.message} class's {@code toCmd()}
     * @param isPractice  Send to the practice server, not tcp network?
     *                {@link ClientNetwork#localTCPServer} is considered "network" here.
     *                Use <tt>isPractice</tt> only with {@link ClientNetwork#practiceServer}.
     * @return true if the message was sent, false if not
     * @throws IllegalArgumentException if {@code s} is {@code null}
     */
//    synchronized boolean put(SOCMessage s, final boolean isPractice)
//        throws IllegalArgumentException
//    {
//        if (s == null)
//            throw new IllegalArgumentException("null");
//
//        return net.send(s);
//    }

    /**
     * request to buy a development card
     *
     * @param ga     the game
     */
    public void buyDevCard(SOCGame ga)
    {
        net.send( new SOCBuyDevCardRequest(ga.getName()) );
    }

    /**
     * request to build something
     *
     * @param ga     the game
     * @param piece  the type of piece, from {@link soc.game.SOCPlayingPiece} constants,
     *               or -1 to request the Special Building Phase.
     * @throws IllegalArgumentException if {@code piece} &lt; -1
     */
    public void buildRequest(SOCGame ga, int piece)
        throws IllegalArgumentException
    {
        net.send( new SOCBuildRequest(ga.getName(), piece) );
    }

    /**
     * request to cancel building something
     *
     * @param ga     the game
     * @param piece  the type of piece, from SOCPlayingPiece constants
     */
    public void cancelBuildRequest(SOCGame ga, int piece)
    {
        net.send( new SOCCancelBuildRequest(ga.getName(), piece));
    }

    /**
     * put a piece on the board, using the {@link SOCPutPiece} message.
     * If the game is in {@link SOCGame#isDebugFreePlacement()} mode,
     * send the {@link SOCDebugFreePlace} message instead.
     *
     * @param ga  the game where the action is taking place
     * @param pp  the piece being placed; {@link SOCPlayingPiece#getCoordinates() pp.getCoordinates()}
     *     and {@link SOCPlayingPiece#getType() pp.getType()} must be >= 0
     * @throws IllegalArgumentException if {@code pp.getType()} &lt; 0 or {@code pp.getCoordinates()} &lt; 0
     */
    public void putPiece(SOCGame ga, SOCPlayingPiece pp)
        throws IllegalArgumentException
    {
        final int co = pp.getCoordinates();
        final SOCMessage ppm = (ga.isDebugFreePlacement())
            ? new SOCDebugFreePlace( ga.getName(), pp.getPlayerNumber(), pp.getType(), co)
            : new SOCPutPiece( ga.getName(), pp.getPlayerNumber(), pp.getType(), co);

        net.send( ppm);
    }

    /**
     * Ask the server to move this piece to a different coordinate.
     * @param ga  the game where the action is taking place
     * @param pn  The piece's player number
     * @param ptype    The piece type, such as {@link SOCPlayingPiece#SHIP}; must be >= 0
     * @param fromCoord  Move the piece from here; must be >= 0
     * @param toCoord    Move the piece to here; must be >= 0
     * @throws IllegalArgumentException if {@code ptype} &lt; 0, {@code fromCoord} &lt; 0, or {@code toCoord} &lt; 0
     * @since 2.0.00
     */
    public void movePieceRequest
        (final SOCGame ga, final int pn, final int ptype, final int fromCoord, final int toCoord)
        throws IllegalArgumentException
    {
        net.send( new SOCMovePiece( ga.getName(), pn, ptype, fromCoord, toCoord));
    }

    /**
     * the player wants to move the robber or the pirate ship.
     *
     * @param ga  the game
     * @param pl  the player
     * @param coord  hex where the player wants the robber, or negative hex for the pirate ship
     */
    public void moveRobber(SOCGame ga, SOCPlayer pl, int coord)
    {
        net.send( new SOCMoveRobber(ga.getName(), pl.getPlayerNumber(), coord));
    }

    /**
     * The player wants to send a simple request to the server, such as
     * {@link SOCSimpleRequest#SC_PIRI_FORT_ATTACK} to attack their
     * pirate fortress in scenario option {@link SOCGameOption#K_SC_PIRI _SC_PIRI}.
     *<P>
     * Using network message request types within the client breaks abstraction,
     * but prevents having a lot of very similar methods for simple requests.
     *
     * @param pl  the requesting player
     * @param reqtype  the request type as defined in {@link SOCSimpleRequest}
     * @since 2.0.00
     * @see #sendSimpleRequest(SOCPlayer, int, int, int)
     */
    public void sendSimpleRequest(final SOCPlayer pl, final int reqtype)
    {
        sendSimpleRequest(pl, reqtype, 0, 0);
    }

    /**
     * The player wants to send a simple request to the server, such as
     * {@link SOCSimpleRequest#SC_PIRI_FORT_ATTACK} to attack their
     * pirate fortress in scenario option {@link SOCGameOption#K_SC_PIRI _SC_PIRI},
     * with optional {@code value1} and {@code value2} parameters.
     *<P>
     * Using network message request types within the client breaks abstraction,
     * but prevents having a lot of very similar methods for simple requests.
     *
     * @param pl  the requesting player
     * @param reqtype  the request type as defined in {@link SOCSimpleRequest}
     * @param value1  First optional detail value, or 0
     * @param value2  Second optional detail value, or 0
     * @since 2.0.00
     * @see #sendSimpleRequest(SOCPlayer, int)
     */
    public void sendSimpleRequest(final SOCPlayer pl, final int reqtype, final int value1, final int value2)
    {
        final SOCGame ga = pl.getGame();
        net.send( new SOCSimpleRequest(ga.getName(), pl.getPlayerNumber(), reqtype, value1, value2) );
    }

    /**
     * send a text message to the people in the game
     *
     * @param ga   the game
     * @param txt  the message text
     * @see MainDisplay#sendToChannel(String, String)
     */
    public void sendText(SOCGame ga, String txt)
    {
        net.send( new SOCGameTextMsg(ga.getName(), "-", txt));
    }

    /**
     * the user leaves the given game
     *
     * @param ga   the game
     */
    public void leaveGame(SOCGame ga)
    {
        clientListeners.remove(ga.getName());
        client.games.remove(ga.getName());
        net.send( new SOCLeaveGame("-", "-", ga.getName()) );
    }

    /**
     * the user sits down to play
     *
     * @param ga   the game
     * @param pn   the number of the seat where the user wants to sit
     */
    public void sitDown(SOCGame ga, int pn)
    {
        net.send( new SOCSitDown(ga.getName(), SOCMessage.EMPTYSTR, pn, false) );
    }

    /**
     * the user wants to start the game
     *
     * @param ga  the game
     */
    public void startGame(SOCGame ga)
    {
        net.send( new SOCStartGame(ga.getName(), 0));
    }

    /**
     * the user rolls the dice
     *
     * @param ga  the game
     */
    public void rollDice(SOCGame ga)
    {
        net.send( new SOCRollDice(ga.getName()));
    }

    /**
     * the user is done with the turn
     *
     * @param ga  the game
     */
    public void endTurn(SOCGame ga)
    {
        net.send( new SOCEndTurn(ga.getName()));
    }

    /**
     * the user wants to discard
     *
     * @param ga  the game
     */
    public void discard(SOCGame ga, SOCResourceSet rs)
    {
        net.send( new SOCDiscard( ga.getName(), -1, rs));
    }

    /**
     * The user has picked these resources to gain from a gold hex,
     * or in game state {@link SOCGame#WAITING_FOR_DISCOVERY} has picked these
     * 2 free resources from a Discovery/Year of Plenty card.
     *
     * @param ga  the game
     * @param rs  The resources to pick
     * @since 2.0.00
     */
    public void pickResources(SOCGame ga, SOCResourceSet rs)
    {
        net.send( new SOCPickResources(ga.getName(), rs));
    }

    /**
     * The user chose a player to steal from,
     * or (game state {@link SOCGame#WAITING_FOR_ROBBER_OR_PIRATE})
     * chose whether to move the robber or the pirate,
     * or (game state {@link SOCGame#WAITING_FOR_ROB_CLOTH_OR_RESOURCE})
     * chose whether to steal a resource or cloth.
     *
     * @param ga  the game
     * @param ch  the player number,
     *   or {@link SOCChoosePlayer#CHOICE_MOVE_ROBBER} to move the robber
     *   or {@link SOCChoosePlayer#CHOICE_MOVE_PIRATE} to move the pirate ship.
     *   See {@link SOCChoosePlayer#SOCChoosePlayer(String, int)} for meaning
     *   of <tt>ch</tt> for game state <tt>WAITING_FOR_ROB_CLOTH_OR_RESOURCE</tt>.
     */
    public void choosePlayer(SOCGame ga, final int ch)
    {
        net.send( new SOCChoosePlayer(ga.getName(), ch));
    }

    /**
     * The user is reacting to the move robber request.
     *
     * @param ga  the game
     */
    public void chooseRobber(SOCGame ga)
    {
        choosePlayer(ga, SOCChoosePlayer.CHOICE_MOVE_ROBBER);
    }

    /**
     * The user is reacting to the move pirate request.
     *
     * @param ga  the game
     */
    public void choosePirate(SOCGame ga)
    {
        choosePlayer(ga, SOCChoosePlayer.CHOICE_MOVE_PIRATE);
    }

    /**
     * the user is rejecting the current offers
     *
     * @param ga  the game
     */
    public void rejectOffer(SOCGame ga)
    {
        net.send( new SOCRejectOffer( ga.getName(), 0));
    }

    /**
     * the user is accepting an offer
     *
     * @param ga  the game
     * @param offeringPN  the number of the player that is making the offer
     */
    public void acceptOffer(SOCGame ga, final int offeringPN)
    {
        net.send( new SOCAcceptOffer( ga.getName(), 0, offeringPN));
    }

    /**
     * the user is clearing an offer
     *
     * @param ga  the game
     */
    public void clearOffer(SOCGame ga)
    {
        net.send( new SOCClearOffer( ga.getName(), 0));
    }

    /**
     * the user wants to trade with the bank or a port.
     *
     * @param ga    the game
     * @param give  what is being offered
     * @param get   what the player wants
     */
    public void bankTrade(SOCGame ga, SOCResourceSet give, SOCResourceSet get)
    {
        net.send( new SOCBankTrade(ga.getName(), give, get, -1));
    }

    /**
     * the user is making an offer to trade with other players.
     *
     * @param ga    the game
     * @param offer the trade offer
     */
    public void offerTrade(SOCGame ga, SOCTradeOffer offer)
    {
        net.send( new SOCMakeOffer( ga.getName(), offer));
    }

    /**
     * the user wants to play a development card
     *
     * @param ga  the game
     * @param dc  the type of development card
     */
    public void playDevCard(SOCGame ga, int dc)
    {
        if ((! ga.isPractice) && (client.sVersion < SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES))
        {
            if (dc == SOCDevCardConstants.KNIGHT)
                dc = SOCDevCardConstants.KNIGHT_FOR_VERS_1_X;
            else if (dc == SOCDevCardConstants.UNKNOWN)
                dc = SOCDevCardConstants.UNKNOWN_FOR_VERS_1_X;
        }
        net.send( new SOCPlayDevCardRequest( ga.getName(), dc));
    }

    /**
     * The current user wants to play a special {@link soc.game.SOCInventoryItem SOCInventoryItem}.
     * Send the server a {@link SOCInventoryItemAction}{@code (currentPlayerNumber, PLAY, itype, rc=0)} message.
     * @param ga     the game
     * @param itype  the special inventory item type picked by player,
     *     from {@link soc.game.SOCInventoryItem#itype SOCInventoryItem.itype}
     */
    public void playInventoryItem(SOCGame ga, final int itype)
    {
        net.send( new SOCInventoryItemAction( ga.getName(), ga.getCurrentPlayerNumber(),
            SOCInventoryItemAction.PLAY, itype, 0));
    }

    /**
     * The current user wants to pick a {@link SOCSpecialItem Special Item}.
     * Send the server a {@link SOCSetSpecialItem}{@code (PICK, typeKey, gi, pi, owner=-1, coord=-1, level=0)} message.
     * @param ga  Game
     * @param typeKey  Special item type.  Typically a {@link SOCGameOption} keyname; see the {@link SOCSpecialItem}
     *     class javadoc for details.
     * @param gi  Game Item Index, as in {@link SOCGame#getSpecialItem(String, int)} or
     *     {@link SOCSpecialItem#playerPickItem(String, SOCGame, SOCPlayer, int, int)}, or -1
     * @param pi  Player Item Index, as in {@link SOCSpecialItem#playerPickItem(String, SOCGame, SOCPlayer, int, int)},
     *     or -1
     */
    public void pickSpecialItem(SOCGame ga, final String typeKey, final int gi, final int pi)
    {
        net.send( new SOCSetSpecialItem
            (ga.getName(), SOCSetSpecialItem.OP_PICK, typeKey, gi, pi, -1));
    }

    /**
     * the client player picked a resource type to monopolize.
     *<P>
     * Before v2.0.00 this method was {@code monopolyPick}.
     *
     * @param ga   the game
     * @param res  the resource type, such as
     *     {@link SOCResourceConstants#CLAY} or {@link SOCResourceConstants#SHEEP}
     */
    public void pickResourceType(SOCGame ga, int res)
    {
        net.send( new SOCPickResourceType(ga.getName(), res));
    }

    /**
     * the user is changing their face image
     *
     * @param ga  the game
     * @param id  the new image id
     */
    public void changeFace(SOCGame ga, int id)
    {
        client.lastFaceChange = id;
        net.send( new SOCChangeFace(ga.getName(), 0, id));
    }

    /**
     * The user is locking or unlocking a seat.
     *
     * @param ga  the game
     * @param pn  the seat number
     * @param sl  new seat lock state; remember that servers older than v2.0.00 won't recognize {@code CLEAR_ON_RESET}
     *     and should be sent {@code UNLOCKED}
     * @since 2.0.00
     */
    public void setSeatLock(SOCGame ga, int pn, SOCGame.SeatLockState sl)
    {
        net.send( new SOCSetSeatLock( ga.getName(), pn, sl));
    }

    /**
     * Player wants to request to reset the board (same players, new game, new layout).
     * Send {@link soc.message.SOCResetBoardRequest} to server;
     * it will either respond with a
     * {@link soc.message.SOCResetBoardAuth} message,
     * or will tell other players to vote yes/no on the request.
     * Before calling, check player.hasAskedBoardReset()
     * and game.getResetVoteActive().
     * @since 1.1.00
     */
    public void resetBoardRequest(SOCGame ga)
    {
        net.send( new SOCResetBoardRequest( ga.getName()));
    }

    /**
     * Player is responding to a board-reset vote from another player.
     * Send {@link soc.message.SOCResetBoardRequest} to server;
     * it will either respond with a
     * {@link soc.message.SOCResetBoardAuth} message,
     * or will tell other players to vote yes/no on the request.
     *
     * @param ga Game to vote on
     * @param voteYes If true, this player votes yes; if false, no
     * @since 1.1.00
     */
    public void resetBoardVote(final SOCGame ga, final boolean voteYes)
    {
        net.send( new SOCResetBoardVote( ga.getName(), 0, voteYes));
    }

    /**
     * Send a {@code :consider-move} command text message to the server
     * asking a robot to show the debug info for
     * a possible move </B>after</B> a move has been made.
     *<P>
     * To show debug info <B>before</B> making a move, see
     * {@link #considerTarget(SOCGame, SOCPlayer, SOCPlayingPiece)}.
     *
     * @param ga  the game
     * @param robotPlayer  the robot player; will call {@link SOCPlayer#getName() getName()}
     * @param piece  the piece type and coordinate to consider
     */
    public void considerMove(final SOCGame ga, final SOCPlayer robotPlayer, final SOCPlayingPiece piece)
    {
        String msg = robotPlayer.getName() + ":consider-move ";  // i18n OK: Is a formatted command to a robot

        switch (piece.getType())
        {
        case SOCPlayingPiece.SETTLEMENT:
            msg += "settlement";
            break;

        case SOCPlayingPiece.ROAD:
            msg += "road";
            break;

        case SOCPlayingPiece.SHIP:
            msg += "ship";
            break;

        case SOCPlayingPiece.CITY:
            msg += "city";
            break;
        }

        msg += (" " + piece.getCoordinates());
        sendText(ga, msg);
    }

    /**
     * Send a {@code :consider-target} command text message to the server
     * asking a robot to show the debug info for
     * a possible move <B>before</B> a move has been made.
     *<P>
     * To show debug info <B>after</B> making a move, see
     * {@link #considerMove(SOCGame, SOCPlayer, SOCPlayingPiece)}.
     *
     * @param ga  the game
     * @param robotPlayer  the robot player; will call {@link SOCPlayer#getName() getName()}
     * @param piece  the piece type and coordinate to consider
     */
    public void considerTarget(final SOCGame ga, final SOCPlayer robotPlayer, final SOCPlayingPiece piece)
    {
        String msg = robotPlayer.getName() + ":consider-target ";  // i18n OK: Is a formatted command to a robot

        switch (piece.getType())
        {
        case SOCPlayingPiece.SETTLEMENT:
            msg += "settlement";
            break;

        case SOCPlayingPiece.ROAD:
            msg += "road";
            break;

        case SOCPlayingPiece.SHIP:
            msg += "ship";
            break;

        case SOCPlayingPiece.CITY:
            msg += "city";
            break;
        }

        msg += (" " + piece.getCoordinates());
        sendText(ga, msg);
    }

}