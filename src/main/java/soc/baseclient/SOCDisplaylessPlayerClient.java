/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2024 Jeremy D Monin <jeremy@nand.net>
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
package soc.baseclient;

import soc.disableDebug.D;

import soc.game.SOCBoard;
import soc.game.SOCBoardLarge;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCSpecialItem;
import soc.game.SOCTradeOffer;

import soc.message.*;

import soc.server.genericServer.StringConnection;
import soc.util.SOCFeatureSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.Socket;

import java.util.HashMap;
import java.util.Hashtable;


/**
 * "Headless" standalone client for connecting to the SOCServer.
 * If you want another connection port, you have to specify it as the "port"
 * argument in the html source. If you run this as a stand-alone, you have to
 * specify the port.
 *<P>
 * The {@link soc.robot.SOCRobotClient SOCRobotClient} is based on this client.
 * Because of this, some methods (such as {@link MessageHandler#handleVERSION( boolean, SOCVersion )})
 * assume the client and server are the same version.
 *<P>
 * Some static methods here are used by {@link soc.client.SOCPlayerClient}
 * and {@link soc.robot.SOCRobotClient}, to prevent code duplication.
 *<P>
 * Since server and built-in robots are the same version, this client doesn't need
 * game option sync messages ({@link SOCGameOptionInfo}) but handles them if sent.
 * Ignores scenario synchronization messages ({@link SOCScenarioInfo}).
 * Being GUI-less, it ignores i18n localization messages ({@link SOCLocalizedStrings}).
 *<P>
 * Before v1.1.20, this class was in the {@code soc.client} package. In 1.1.20,
 * for server jar packaging it was moved into a new {@code soc.baseclient} package.
 *
 * As of 16 Aug 2024 this class is only used by robots.
 *
 * @author Robert S Thomas
 */
public class SOCDisplaylessPlayerClient extends SOCBaseClient implements Runnable
{
    /**
     * Flag property <tt>jsettlers.debug.traffic</tt>: When present, the
     * contents of incoming and outgoing network message traffic should be debug-printed.
     * Used by this class, {@link soc.robot.SOCRobotClient SOCRobotClient}, and
     * {@link soc.client.SOCPlayerClient SOCPlayerClient}.
     * @since 1.2.00
     */
    public static final String PROP_JSETTLERS_DEBUG_TRAFFIC = "jsettlers.debug.traffic";

    /**
     * For client/robot testing, string property {@code "jsettlers.debug.client.gameopt3p"}
     * with name of a "third-party" Known Game Option to create; will have {@link SOCGameOption#FLAG_3RD_PARTY}.
     * At server connect, will report having client feature {@code "com.example.js.feat."} + optionName
     * along with default features or {@link soc.client.SOCPlayerClient#PROP_JSETTLERS_DEBUG_CLIENT_FEATURES}
     * in the {@link SOCVersion} message sent to server. See {@link SOCFeatureSet} for more info.
     *
     * @see soc.server.SOCServer#PROP_JVM_JSETTLERS_DEBUG_SERVER_GAMEOPT3P
     * @since 2.5.00
     */
    public static final String PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P = "jsettlers.debug.client.gameopt3p";

    protected static String STATSPREFEX = "  [";
    protected String doc;
    protected String lastMessage;

    /**
     * Time when next {@link SOCServerPing} is expected at, based on
     * previous ping's {@link SOCServerPing#getSleepTime()}, or 0.
     * Repetitive pings' {@link #debugTraffic} prints should be hidden
     * when the bot is otherwise idle. Receiving other in-game message types
     * resets this to 0.
     * @since 2.0.00
     */
    protected long nextServerPingExpectedAt;

    /**
     * Server connection info.
     * Robot clients should set non-null {@link ServerConnectInfo#robotCookie} when constructing.
     *<P>
     * Versions before 2.2.00 instead had {@code host}, {@code port}, {@code strSocketName} fields.
     *
     * @see #allOptsReceived
     * @since 2.2.00
     */
    protected final ServerConnectInfo serverConnectInfo;

    /**
     * This client's set of Known Options.
     * Initialized from {@link SOCGameOptionSet#getAllKnownOptions()}, updated by
     * {@link SOCGameOptionInfo} messages.
     * @see #allOptsReceived
     * @see MessageHandler#handleGAMEOPTIONINFO(SOCGameOptionInfo)
     * @since 2.5.00
     */
    public SOCGameOptionSet knownOpts = SOCGameOptionSet.getAllKnownOptions();

    /**
     * Since server and built-in robots are the same version,
     * this client doesn't need {@link SOCGameOption} synchronization messages.
     * Server still sends them in some conditions, such as when gameopts are limited by client features
     * or when inactive opts have been activated.
     *<P>
     * By default assumes true, there's no pending info to receive, because is same version as server.
     * If a gameopt sync message is sent, will set false until the "end of list" marker message is sent.
     *
     * @see #knownOpts
     * @see MessageHandler#handleGAMEOPTIONINFO(SOCGameOptionInfo)
     * @since 2.5.00
     */
    protected boolean allOptsReceived = true;

    /**
     * Network socket. Initialized in subclasses.
     * Before v2.5.00 this field was {@code s}.
     */
    protected Socket sock;

    protected DataInputStream in;
    protected DataOutputStream out;

    /**
     * Local server connection, if {@link ServerConnectInfo#stringSocketName} != null.
     * @see #sLocalVersion
     * @since 1.1.00
     */
    protected StringConnection sLocal;

    /**
     * Server version number, sent soon after connect, or -1 if unknown.
     * {@link #sLocalVersion} should always equal our own version.
     * @since 1.1.00
     */
    public int sLocalVersion;

    /**
     * Server's active optional features, sent soon after connect, or null if unknown.
     * {@link #sLocalFeatures} goes with our locally hosted server, if any.
     * @since 1.1.19
     */
//    protected SOCFeatureSet sFeatures, sLocalFeatures;

    protected Thread reader = null;
    protected Exception ex = null;
    public boolean connected = false;

    /**
     * were we rejected from server? (full or robot name taken)
     * @since 1.1.00
     */
    protected boolean rejected = false;

    /**
     * the nickname
     */
//    protected String nickname = null;

    /**
     * the password
     */
//    protected String password = null;

    /**
     * true if we've stored the password
     */
    protected boolean gotPassword;

    /**
     * Chat channels; for general use, or possibly in a future version to control bots.
     */
    protected Hashtable<String,?> channels = new Hashtable<String,Object>();

    /**
     * the games we're playing
     *<P>
     * Games are added by {@link MessageHandler#handleJOINGAMEAUTH(SOCJoinGameAuth)},
     * removed by {@link #leaveGame(String)}.
     * @see #getGame(String)
     */
//    protected Hashtable<String, SOCGame> games = new Hashtable<String, SOCGame>();


    /**
     * Should client ignore all {@link SOCPlayerStats} messages,
     * although they may be about our {@link #nickname} client player? True by default.
     * @see MessageHandler#handlePLAYERSTATS(SOCPlayerStats, SOCGame, int)
     * @since 2.7.00
     */
    protected boolean ignorePlayerStats = true;

    /**
     * Constructor to set up using this server connect info. Does not actually connect here;
     * subclass methods such as {@link soc.robot.SOCRobotClient#init()} must do so.
     * 
     * @param sci  Server connect info (TCP or stringPort); not {@code null}
     * @param visual  true if this client is visual
     * @throws IllegalArgumentException if {@code sci == null}
     * @since 2.2.00
     */
    public SOCDisplaylessPlayerClient(ServerConnectInfo sci, boolean visual)
        throws IllegalArgumentException
    {
        if (sci == null)
            throw new IllegalArgumentException("sci");
        this.serverConnectInfo = sci;

        sVersion = -1; //  sLocalVersion = -1;

//        if (null != System.getProperty(PROP_JSETTLERS_DEBUG_TRAFFIC))
            debugTraffic = true;  // set flag if debug prop has any value at all
    }

    /**
     * @return the nickname of this user
     */
    public String getNickname()
    {
        return nickname;
    }

    /**
     * Get a game that we've joined in this client.
     * @param gaName  game name to look for
     * @return Game if we've joined it, or {@code null} if not found or not joined
     * @since 2.5.00
     */
    public SOCGame getGame(final String gaName)
    {
        return games.get(gaName);
    }

    /**
     * Continuously read from the net in a separate thread.
     * if {@link java.io.EOFException} or another error occurs, breaks the loop:
     * Exception will be stored in {@link #ex}. {@link #destroy()} will be called
     * unless {@code ex} is an {@link InterruptedIOException} (socket timeout).
     */
    public void run()
    {
        // Thread name for debug
        try
        {
            Thread.currentThread().setName("robot-netread-" + nickname);
        }
        catch (Throwable th) {}

        try
        {
            while (connected)
            {
                String s;
                if (sLocal == null)
                    s = in.readUTF();
                else
                    s = sLocal.readNext();

                SOCMessage msg = SOCMessage.toMsg(s);
                if (msg != null)
                    messageHandler.handle( msg, false );
                else if (debugTraffic)
                    soc.debug.D.ebugERROR(nickname + ": Could not parse net message: " + s);
            }
        }
        catch (InterruptedIOException e)
        {
            ex = e;
            System.err.println("Socket timeout in run: " + e);
        }
        catch (IOException e)
        {
            if (! connected)
            {
                return;
            }

            ex = e;
            destroy();
        }
    }

    /**
     * resend the last message
     */
    public void resend()
    {
        if (lastMessage != null)
            put(lastMessage);
    }

    /**
     * write a message to the net
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     * @throws IllegalArgumentException if {@code s} is {@code null}
     */
    public synchronized boolean put(String s)
        throws IllegalArgumentException
    {
        if (s == null)
            throw new IllegalArgumentException("null");

        lastMessage = s;

        if (debugTraffic || D.ebugIsEnabled())
            soc.debug.D.ebugPrintlnINFO("OUT - " + nickname + " - " + s);

        if ((ex != null) || ! connected)
        {
            return false;
        }

        try
        {
            if (sLocal == null)
            {
                out.writeUTF(s);
                out.flush();
            } else {
                sLocal.put(s);
            }
        }
        catch (InterruptedIOException x)
        {
            System.err.println("Socket timeout in put: " + x);
        }
        catch (IOException e)
        {
            ex = e;
            System.err.println("could not write to the net: " + ex);
            destroy();

            return false;
        }

        return true;
    }
    
    
    /** END HANDLE CASE **/
    
    /**
     * handle the "join channel authorization" message.
     * @param mes  the message
     */
//    protected void handleJOINCHANNELAUTH(SOCJoinChannelAuth mes)
//    {
//        gotPassword = true;
//    }


    /**
     * handle the "a client joined a channel" message.
     * @param mes  the message
     */
//    protected void handleJOINCHANNEL(SOCJoinChannel mes) {}

    /**
     * handle the "channel members" message.
     * @param mes  the message
     */
//    protected void handleCHANNELMEMBERS(SOCChannelMembers mes) {}

    /**
     * handle the "new channel" message
     * @param mes  the message
     */
//    protected void handleNEWCHANNEL(SOCNewChannel mes) {}

    /**
     * handle the "list of channels" message
     * @param mes  the message
     */
    protected void handleCHANNELS(SOCChannels mes) {}

    /**
     * handle a broadcast text message
     * @param mes  the message
     */
    protected void handleBCASTTEXTMSG(SOCBCastTextMsg mes) {}

    /**
     * handle a text message
     * @param mes  the message
     */
    protected void handleCHANNELTEXTMSG(SOCChannelTextMsg mes) {}

    /**
     * handle the "leave channel" message
     * @param mes  the message
     */
    protected void handleLEAVECHANNEL(SOCLeaveChannel mes) {}

    /**
     * handle the "delete channel" message
     * @param mes  the message
     */
    protected void handleDELETECHANNEL(SOCDeleteChannel mes) {}

    /**
     * handle the "list of games" message
     * @param mes  the message
     */
    protected void handleGAMES(SOCGames mes) {}

    /**
     * handle the "list of games with options" message
     * @since 2.5.00
     */
    protected void handleGAMESWITHOPTIONS(final SOCGamesWithOptions mes) {}

    /**
     * handle the "join game" message
     * @param mes  the message
     */
    protected void handleJOINGAME(SOCJoinGame mes) {}

    /**
     * handle the "leave game" message
     * @param mes  the message
     */
    protected void handleLEAVEGAME(SOCLeaveGame mes)
    {
        String gn = (mes.getGameName());
        SOCGame ga = games.get(gn);
        if (ga == null)
            return;

        SOCPlayer player = ga.getPlayer(mes.getNickname());

        if (player != null)
        {
            //  This user was not a spectator
            ga.removePlayer(mes.getNickname(), false);
        }
    }

    /**
     * handle the "new game" message
     * @param mes  the message
     */
    protected void handleNEWGAME(SOCNewGame mes) {}

    /**
     * handle the "new game with options" message
     * @since 2.5.00
     */
    protected void handleNEWGAMEWITHOPTIONS(final SOCNewGameWithOptions mes) {}

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
//    protected void handleDELETEGAME(SOCDeleteGame mes) {}

    /**
     * handle the "game members" message
     * @param mes  the message
     */
    /**
     * handle the "game text message" message; stub.
     * Overridden by bot to look for its debug commands.
     * @param mes  the message
     */
//    protected void handleGAMETEXTMSG(SOCGameTextMsg mes) {}

    /**
     * Handle the "game server text" message; stub.
     * Ignored by bots. This stub can be overridden by future subclasses.
     * @param mes  the message
     */
    protected void handleGAMESERVERTEXT(SOCGameServerText mes) {}

      /**
     * handle the "board layout" message, new format
     * @param mes  the message
     * @param ga  Game to apply layout to, for method reuse; does nothing if null
     * @since 1.1.08
     * @return True if layout was understood and game != null, false otherwise
     * @see MessageHandler#handleBOARDLAYOUT(SOCBoardLayout, SOCGame)
     */
    public static boolean handleBOARDLAYOUT2(SOCBoardLayout2 mes, final SOCGame ga)
    {
        if (ga == null)
            return false;

        SOCBoard bd = ga.getBoard();
        final int bef = mes.getBoardEncodingFormat();
        if (bef == SOCBoard.BOARD_ENCODING_LARGE)
        {
            // v3
            ((SOCBoardLarge) bd).setLandHexLayout(mes.getIntArrayPart("LH"));
            ga.setPlayersLandHexCoordinates();
            int hex = mes.getIntPart("RH");
            if (hex != 0)
                bd.setRobberHex(hex, false);
            hex = mes.getIntPart("PH");
            if (hex != 0)
                ((SOCBoardLarge) bd).setPirateHex(hex, false);
            int[] portLayout = mes.getIntArrayPart("PL");
            if (portLayout != null)
                bd.setPortsLayout(portLayout);
            int[] x = mes.getIntArrayPart("PX");
            if (x != null)
                ((SOCBoardLarge) bd).setPlayerExcludedLandAreas(x);
            x = mes.getIntArrayPart("RX");
            if (x != null)
                ((SOCBoardLarge) bd).setRobberExcludedLandAreas(x);
            x = mes.getIntArrayPart("CV");
            if (x != null)
                ((SOCBoardLarge) bd).setVillageAndClothLayout(x);
            x = mes.getIntArrayPart("LS");
            if (x != null)
                ((SOCBoardLarge) bd).addLoneLegalSettlements(ga, x);

            HashMap<String, int[]> others = mes.getAddedParts();
            if (others != null)
                ((SOCBoardLarge) bd).setAddedLayoutParts(others);
        }
        else if (bef <= SOCBoard.BOARD_ENCODING_6PLAYER)
        {
            // v1 or v2
            bd.setHexLayout(mes.getIntArrayPart("HL"));
            bd.setNumberLayout(mes.getIntArrayPart("NL"));
            bd.setRobberHex(mes.getIntPart("RH"), false);
            int[] portLayout = mes.getIntArrayPart("PL");
            if (portLayout != null)
                bd.setPortsLayout(portLayout);
        } else {
            // Should not occur: Server has sent an unrecognized format
            System.err.println
                ("Cannot recognize game encoding v" + bef + " for game " + ga.getName());
            return false;
        }

        ga.updateAtBoardLayout();
        return true;
    }

    /**
     * handle the "discard request" message
     * @param mes  the message
     */
    protected void handleDISCARDREQUEST(SOCDiscardRequest mes) {}

    /**
     * handle the "choose player request" message
     * @param mes  the message
     */
    protected void handleCHOOSEPLAYERREQUEST(SOCChoosePlayerRequest mes) {}

     /**
     * handle the "make offer" message.
     * @param mes  the message
     */
    protected void handleMAKEOFFER(SOCMakeOffer mes)
    {
        SOCGame ga = games.get(mes.getGameName());
        if (ga == null)
            return;

        SOCTradeOffer offer = mes.getOffer();
        ga.getPlayer(offer.getFrom()).setCurrentOffer(offer);
    }

    /**
     * handle the "reject offer" message
     * @param mes  the message
     */
    protected void handleREJECTOFFER(SOCRejectOffer mes) {}

    /**
     * handle the "clear trade message" message
     * @param mes  the message
     */
    protected void handleCLEARTRADEMSG(SOCClearTradeMsg mes) {}

      /**
     * send a text message to a chat channel
     *
     * @param ch   the name of the channel
     * @param mes  the message
     */
    public void chSend(String ch, String mes)
    {
        put(new SOCChannelTextMsg(ch, nickname, mes).toCmd());
    }

    /**
     * the user leaves the given chat channel
     *
     * @param ch  the name of the channel
     */
    public void leaveChannel(String ch)
    {
        channels.remove(ch);
        put(SOCLeaveChannel.toCmd(nickname, "-", ch));
    }

    /**
     * disconnect from the net, and from any local practice server
     * Called by {@link #destroy()}.
     */
    public void disconnect()
    {
        connected = false;

        // reader will die once 'connected' is false, and socket is closed

        if (sLocal != null)
            sLocal.disconnect();

        try
        {
            sock.close();
        }
        catch (Exception e)
        {
            ex = e;
        }
    }
    
    @Override
    public void requestAuthorization()
    {
    
    }
    
    /**
     * Ask to join a game; robots don't send this, server instead tells them to join games.
     * Useful for testing and maybe third-party clients.
     * @param gaName  Name of game to ask to join
     * @since 2.5.00
     */
    public void joinGame(final String gaName)
    {
        put(new SOCJoinGame("-", "", "-", gaName).toCmd());
    }

    /**
     * request to buy a development card
     *
     * @param ga     the game
     */
    public void buyDevCard(SOCGame ga)
    {
        put(new SOCBuyDevCardRequest(ga.getName()).toCmd());
    }

    /**
     * Request to build something or ask for the 6-player Special Building Phase (SBP).
     *
     * @param ga     the game
     * @param piece  the type of piece, from {@link soc.game.SOCPlayingPiece} constants,
     *               or -1 to request the Special Building Phase.
     * @throws IllegalArgumentException if {@code piece} &lt; -1
     */
    public void buildRequest(SOCGame ga, int piece)
        throws IllegalArgumentException
    {
        put(new SOCBuildRequest(ga.getName(), piece).toCmd());
    }

    /**
     * request to cancel building something or playing a dev card
     *
     * @param ga     the game
     * @param piece  the type of piece from SOCPlayingPiece, or {@link SOCCancelBuildRequest#CARD}
     */
    public void cancelBuildRequest(SOCGame ga, int piece)
    {
        put(new SOCCancelBuildRequest(ga.getName(), piece).toCmd());
    }

    /**
     * Send request to put a piece on the board.
     *
     * @param ga  the game where the action is taking place
     * @param pp  the piece being placed; {@link SOCPlayingPiece#getCoordinates() pp.getCoordinates()}
     *     and {@link SOCPlayingPiece#getType() pp.getType()} must be >= 0
     * @throws IllegalArgumentException if {@code pp.getCoordinates()} &lt; 0
     *     or {@code pp.getType()} &lt; 0
     */
    public void putPiece(SOCGame ga, SOCPlayingPiece pp)
        throws IllegalArgumentException
    {
        final int pt = pp.getType();
        if (pt < 0)
            throw new IllegalArgumentException("pt: " + pt);

        /**
         * send the command
         */
        put(SOCPutPiece.toCmd(ga.getName(), pp.getPlayerNumber(), pt, pp.getCoordinates()));
    }

    /**
     * Ask the server to move this piece to a different coordinate.
     * @param ga  the game where the action is taking place
     * @param pn  The piece's player number
     * @param ptype    The piece type, such as {@link SOCPlayingPiece#SHIP}; must be >= 0
     * @param fromCoord  Move the piece from here; must be >= 0
     * @param toCoord    Move the piece to here; must be >= 0
     * @throws IllegalArgumentException if {@code ptype} &lt; 0, {@code fromCoord} &lt; 0, or {@code toCoord} &lt; 0
     * @since 2.5.00
     */
    public void movePieceRequest
        (final SOCGame ga, final int pn, final int ptype, final int fromCoord, final int toCoord)
        throws IllegalArgumentException
    {
        put(new SOCMovePiece(ga.getName(), pn, ptype, fromCoord, toCoord).toCmd());
    }

    /**
     * Ask the server to undo placing or moving a piece.
     * @param ga  game where the action is taking place; will call {@link SOCGame#getCurrentPlayerNumber()}
     * @param ptype  piece type, such as {@link SOCPlayingPiece#SHIP}; must be &gt;= 0
     * @param coord  coordinate where piece was placed or moved to; must be &gt; 0
     * @param movedFromCoord  if undoing a move, the coordinate where piece was moved from, otherwise 0
     * @throws IllegalArgumentException if {@code ptype} &lt; 0, {@code coord} &lt;= 0, or {@code movedFromCoord} &lt; 0
     * @since 2.7.00
     */
    public void undoPutOrMovePieceRequest
        (final SOCGame ga, final int ptype, final int coord, final int movedFromCoord)
        throws IllegalArgumentException
    {
        put(new SOCUndoPutPiece(ga.getName(), ga.getCurrentPlayerNumber(), ptype, coord, movedFromCoord).toCmd());
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
        put(SOCMoveRobber.toCmd(ga.getName(), pl.getPlayerNumber(), coord));
    }

    /**
     * Send a {@link SOCSimpleRequest} to the server.
     * {@code reqType} gives the request type, and the optional
     * {@code value1} and {@code value2} depend on request type.
     *
     * @param ga  the game
     * @param ourPN  our player's player number
     * @param reqType  Request type, such as {@link SOCSimpleRequest#SC_PIRI_FORT_ATTACK}.
     *        See {@link SOCSimpleRequest} public int fields for possible types and their meanings.
     * @param value1  First optional detail value, or 0
     * @param value2  Second optional detail value, or 0
     * @since 2.0.00
     */
    public void simpleRequest
        (final SOCGame ga, final int ourPN, final int reqType, final int value1, final int value2)
    {
        put(SOCSimpleRequest.toCmd(ga.getName(), ourPN, reqType, value1, value2));
    }

    /**
     * Send a request to pick a {@link SOCSpecialItem Special Item}, using a
     * {@link SOCSetSpecialItem}{@code (PICK, typeKey, gi, pi, owner=-1, coord=-1, level=0)} message.
     * @param ga  Game
     * @param typeKey  Special item type.  Typically a {@link SOCGameOption} keyname; see the {@link SOCSpecialItem}
     *     class javadoc for details.
     * @param gi  Game Item Index, as in {@link SOCGame#getSpecialItem(String, int)} or
     *     {@link SOCSpecialItem#playerPickItem(String, SOCGame, SOCPlayer, int, int)}, or -1
     * @param pi  Player Item Index, as in {@link SOCSpecialItem#playerPickItem(String, SOCGame, SOCPlayer, int, int)},
     *     or -1
     * @since 2.0.00
     */
    public void pickSpecialItem(SOCGame ga, final String typeKey, final int gi, final int pi)
    {
        put(new SOCSetSpecialItem(ga.getName(), SOCSetSpecialItem.OP_PICK, typeKey, gi, pi, -1).toCmd());
    }

    /**
     * Send a request to play a {@link soc.game.SOCInventoryItem SOCInventoryItem}
     * (not a standard {@link soc.game.SOCDevCard SOCDevCard}) using a
     * {@link SOCInventoryItemAction}{@code (ourPN, PLAY, itype, rc=0)} message.
     * @param ga     the game
     * @param ourPN  our player's player number
     * @param itype  the special inventory item type picked by player,
     *     from {@link soc.game.SOCInventoryItem#itype SOCInventoryItem.itype}
     * @since 2.0.00
     */
    public void playInventoryItem(SOCGame ga, final int ourPN, final int itype)
    {
        put(SOCInventoryItemAction.toCmd
            (ga.getName(), ourPN, SOCInventoryItemAction.PLAY, itype, 0));
    }

    /**
     * send a text message to the people in the game
     *
     * @param ga   the game; does nothing if {@code null}
     * @param me   the message
     */
    public void sendText(SOCGame ga, String me)
    {
        if (ga == null)
            return;

        put(new SOCGameTextMsg(ga.getName(), nickname, me).toCmd());
    }
    
    /**
     * user wants to leave a game.
     * Calls {@link #leaveGame(String)}.
     *
     * @param ga   the game
     */
    public void leaveGame(SOCGame ga)
    {
        leaveGame(ga.getName());
    }
    
    @Override
    protected SOCGameOptionSet getKnownOpts( boolean isPracticeServer )
    {
        return null;
    }
    
    @Override
    protected boolean doesGameExist( String gameName, boolean checkPractice )
    {
        return false;
    }
    
    @Override
    protected SOCGameOptionSet getGameOptions( String gameName )
    {
        return null;
    }
    
    @Override
    public int getServerVersion( SOCGame game )
    {
        return game.serverVersion;
    }
    
    @Override
    public void shutdownFromNetwork()
    {
    
    }
    
    /**
     * user wants to leave a game, which they may not have been able to fully join.
     * Also removes game from client's game list; {@link #getGame(String)} would return {@code null} for it afterwards.
     *
     * @param gaName  the game name
     * @see #leaveGame(SOCGame)
     * @since 2.5.00
     */
    public void leaveGame(final String gaName)
    {
        games.remove(gaName);
        put(new SOCLeaveGame(nickname, "-", gaName).toCmd());
    }

    /**
     * User is sending server a request to sit down to play.
     *
     * @param ga   the game
     * @param pn   the number of the seat where the user wants to sit
     */
    public void sitDown(SOCGame ga, int pn)
    {
        put(SOCSitDown.toCmd(ga.getName(), SOCMessage.EMPTYSTR, pn, false));
    }

    /**
     * the user is starting the game
     *
     * @param ga  the game
     */
    public void startGame(SOCGame ga)
    {
        put(SOCStartGame.toCmd(ga.getName(), 0));
    }

    /**
     * the user rolls the dice
     *
     * @param ga  the game
     */
    public void rollDice(SOCGame ga)
    {
        put(SOCRollDice.toCmd(ga.getName()));
    }

    /**
     * the user is done with the turn
     *
     * @param ga  the game
     */
    public void endTurn(SOCGame ga)
    {
        put(SOCEndTurn.toCmd(ga.getName()));
    }

    /**
     * the user wants to discard
     *
     * @param ga  the game
     * @param rs  Resources to discard
     */
    public void discard(SOCGame ga, SOCResourceSet rs)
    {
        put(new SOCDiscard(ga.getName(), -1, rs).toCmd());
    }

    /**
     * The user chose a player to steal from,
     * or (game state {@link SOCGame#WAITING_FOR_ROBBER_OR_PIRATE})
     * chose whether to move the robber or the pirate,
     * or (game state {@link SOCGame#WAITING_FOR_ROB_CLOTH_OR_RESOURCE})
     * chose whether to steal a resource or cloth.
     *<P>
     * If choice is from playing a {@link SOCDevCardConstants#KNIGHT} card, to cancel the card
     * call {@link #cancelBuildRequest(SOCGame, int) cancelBuildRequest}({@link SOCCancelBuildRequest#CARD}) instead.
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
        put(SOCChoosePlayer.toCmd(ga.getName(), ch));
    }

    /**
     * the user is rejecting the current offers
     *
     * @param ga  the game
     */
    public void rejectOffer(SOCGame ga)
    {
        put(new SOCRejectOffer(ga.getName(), 0).toCmd());
    }

    /**
     * the user is accepting an offer
     *
     * @param ga  the game
     * @param from the number of the player that is making the offer
     */
    public void acceptOffer(SOCGame ga, int from)
    {
        put(new SOCAcceptOffer(ga.getName(), 0, from).toCmd());
    }

    /**
     * the user is clearing an offer
     *
     * @param ga  the game
     */
    public void clearOffer(SOCGame ga)
    {
        put(SOCClearOffer.toCmd(ga.getName(), 0));
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
        put(new SOCBankTrade(ga.getName(), give, get, -1).toCmd());
    }

    /**
     * the user is making an offer to trade with other players.
     *
     * @param ga    the game
     * @param offer the trade offer
     */
    public void offerTrade(SOCGame ga, SOCTradeOffer offer)
    {
        put(SOCMakeOffer.toCmd(ga.getName(), offer));
    }

    /**
     * the user wants to play a development card
     *
     * @param ga  the game
     * @param dc  the type of development card
     */
    public void playDevCard(SOCGame ga, int dc)
    {
        if ((! ga.isPractice) && (sVersion < SOCDevCardConstants.VERSION_FOR_RENUMBERED_TYPES))
        {
            // Unlikely; the displayless client is currently used for SOCRobotClient,
            // and the built-in robots must be the same version as the server.
            // This code is here for a third-party bot or other user of displayless.

            if (dc == SOCDevCardConstants.KNIGHT)
                dc = SOCDevCardConstants.KNIGHT_FOR_VERS_1_X;
            else if (dc == SOCDevCardConstants.UNKNOWN)
                dc = SOCDevCardConstants.UNKNOWN_FOR_VERS_1_X;
        }
        put(SOCPlayDevCardRequest.toCmd(ga.getName(), dc));
    }

    /**
     * the user picked 2 resources to discover (Discovery/Year of Plenty),
     * or picked these resources to gain from a gold hex.
     *<P>
     * Before v2.0.00, this method was {@code discoveryPick(..)}.
     *
     * @param ga    the game
     * @param rscs  the resources
     */
    public void pickResources(SOCGame ga, SOCResourceSet rscs)
    {
        put(new SOCPickResources(ga.getName(), rscs).toCmd());
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
        put(new SOCPickResourceType(ga.getName(), res).toCmd());
    }

    /**
     * the user is changing the face image
     *
     * @param ga  the game
     * @param id  the image id
     */
    public void changeFace(SOCGame ga, int id)
    {
        put(new SOCChangeFace(ga.getName(), ga.getPlayer(nickname).getPlayerNumber(), id).toCmd());
    }

    /**
     * The user is locking or unlocking a seat.
     *
     * @param ga  the game
     * @param pn  the seat number
     * @param sl  new seat lock state; remember that servers older than v2.0.00 won't recognize {@code CLEAR_ON_RESET}
     * @since 2.0.00
     */
    public void setSeatLock(SOCGame ga, int pn, SOCGame.SeatLockState sl)
    {
        put(SOCSetSeatLock.toCmd(ga.getName(), pn, sl));
    }

    /**
     * Connection to server has raised an error that wasn't {@link InterruptedIOException};
     * {@link #ex} contains exception detail. Leave all games, then disconnect.
     *<P>
     * {@link soc.robot.SOCRobotClient} overrides this to try and reconnect.
     */
    public void destroy()
    {
        SOCLeaveAll leaveAllMes = new SOCLeaveAll();
        put(leaveAllMes.toCmd());
        disconnect();
    }

    /**
     * for stand-alones
     */
//    public static void main(String[] args)
//    {
//        SOCDisplaylessPlayerClient ex1 = new SOCDisplaylessPlayerClient
//            (new ServerConnectInfo(args[0], Integer.parseInt(args[1]), null), true);
//        new Thread(ex1).start();
//        Thread.yield();
//    }

}
