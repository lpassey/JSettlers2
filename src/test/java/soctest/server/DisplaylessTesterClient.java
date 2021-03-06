/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2020 Jeremy D Monin <jeremy@nand.net>
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

package soctest.server;

import soc.baseclient.SOCDisplaylessPlayerClient;
import soc.baseclient.ServerConnectInfo;
import soc.communication.Connection;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.message.*;
import soc.communication.MemServerSocket;
import soc.server.SOCServerGameOptionSet;
import soc.util.SOCFeatureSet;
import soc.util.SOCGameList;
import soc.util.Version;

/**
 * Non-testing class: Robot utility client to help run the actual tests.
 * Works with {@link RecordingTesterServer}.
 * Debug Traffic flag is set, which makes unit test logs larger but is helpful when troubleshooting.
 * Unlike parent class, this client connects and authenticates as a "human" player, not a bot,
 * to see same messages a human would be shown.
 * To help set a known test environment, always uses locale {@code "en_US"} unless constructor says otherwise.
 *
 * @since 2.4.50
 */
public class DisplaylessTesterClient extends SOCDisplaylessPlayerClient
{
    /**
     * Locale sent in {@link #init()}, or {@code null} for {@code "en_US"}
     */
    protected String localeStr;

    /**
     * Track server's games and options like SOCPlayerClient does,
     * instead of ignoring them until joined like SOCRobotClient.
     *<P>
     * This field is null until {@link MessageHandler#handleGAMES(SOCGames, boolean) handleGAMES},
     *   {@link MessageHandler#handleGAMESWITHOPTIONS(SOCGamesWithOptions, boolean) handleGAMESWITHOPTIONS},
     *   {@link MessageHandler#handleNEWGAME(SOCNewGame, boolean) handleNEWGAME}
     *   or {@link MessageHandler#handleNEWGAMEWITHOPTIONS(SOCNewGameWithOptions, boolean) handleNEWGAMEWITHOPTIONS}
     *   is called.
     */
    protected SOCGameList serverGames;

    /**
     * Constructor for a displayless client which will connect to a local server.
     * Does not actually connect here: Call {@link #init()} when ready.
     *
     * @param localeStr  Locale to test with, or {@code null} to use {@code "en_US"}
     * @param knownOpts  Known Options, or {@code null} to use defaults from {@link SOCDisplaylessPlayerClient}
     */
    public DisplaylessTesterClient( final String stringport, final String nickname,
        final String localeStr, final SOCGameOptionSet knownOpts)
    {
        super(new ServerConnectInfo( stringport, null ));

        this.nickname = nickname;
        this.localeStr = localeStr;
        if (knownOpts != null)
            this.knownOpts = knownOpts;
        else
            this.knownOpts = SOCServerGameOptionSet.getAllKnownOptions();
        serverGames = new SOCGameList( this.knownOpts );
        debugTraffic = true;
    }

    @Override
    public void dispatch( SOCMessage message, Connection connection ) throws IllegalStateException
    {
        treat( message, false );
    }

    @Override       // the client side doesn't need to discriminate between first and subsequent messages.
    public void dispatchFirst( SOCMessage message, Connection connection ) throws IllegalStateException
    {
        treat( message, false );
    }

    /**
     * Initialize the displayless client; connect to server and send first messages
     * including our version, features from {@link #buildClientFeats()}, and {@link #rbclass}.
     * If fails to connect, sets {@link #ex} and prints it to {@link System#err}.
     * Based on {@link soc.robot.SOCRobotClient#init()}.
     *<P>
     * When done testing, caller should use {@link SOCDisplaylessPlayerClient#destroy()} to shut down.
     */
    public void init()
    {
        try
        {
            connection = MemServerSocket.connectTo( Connection.JVM_STRINGPORT, null, false );
            connected = true;
            connection.startMessageProcessing( this );

            connection.send(new SOCVersion
                (Version.versionNumber(), Version.version(), Version.buildnum(),
                 buildClientFeats().getEncodedList(),
                 (localeStr != null) ? localeStr : "en_US") );
            connection.send(new SOCAuthRequest
                (SOCAuthRequest.ROLE_GAME_PLAYER, nickname, "",
                 SOCAuthRequest.SCHEME_CLIENT_PLAINTEXT, "-") );
        }
        catch (Exception e)
        {
            ex = e;
            System.err.println("Could not connect to the server: " + ex);
        }
    }

    // from SOCRobotClient ; TODO combine common later?
    protected SOCFeatureSet buildClientFeats()
    {
        SOCFeatureSet feats = new SOCFeatureSet(false, false);
        feats.add(SOCFeatureSet.CLIENT_6_PLAYERS);
        feats.add(SOCFeatureSet.CLIENT_SEA_BOARD);
        feats.add(SOCFeatureSet.CLIENT_SCENARIO_VERSION, Version.versionNumber());

        return feats;
    }

    /**
     * To show successful connection, get the server's version.
     * Same format as {@link soc.util.Version#versionNumber()}.
     */
    public int getServerVersion()
    {
        return connection.getRemoteVersion();
    }

    /** Ask to join a game; must have authed already. Sends {@link SOCJoinGame}. */
    public void askJoinGame(String gaName)
    {
        connection.send( new SOCJoinGame(nickname, "", SOCMessage.EMPTYSTR, gaName) );
    }

    // message handlers

    /** To avoid confusion during gameplay, set both "server" version fields */
    @Override
    protected void handleVERSION(boolean isLocal, SOCVersion mes)
    {
        connection.setVersion( mes.getVersionNumber(), true );
    }

    // TODO: refactor common with SOCPlayerClient vs this and its displayless parent,
    // which currently don't share a parent client class with SOCPlayerClient

    @Override
    protected void handleGAMES(final SOCGames mes)
    {
        serverGames.addGames(mes.getGames(), Version.versionNumber());
    }

    @Override
    protected void handleGAMESWITHOPTIONS(final SOCGamesWithOptions mes)
    {
        serverGames.addGames(mes.createNewGameList(knownOpts), Version.versionNumber());
    }

    @Override
    protected void handleNEWGAME(final SOCNewGame mes)
    {
        String gameName = mes.getGameName();
        boolean canJoin = true;
        boolean hasUnjoinMarker = (gameName.charAt(0) == SOCGames.MARKER_THIS_GAME_UNJOINABLE);
        if (hasUnjoinMarker)
        {
            gameName = gameName.substring(1);
            canJoin = false;
        }
        serverGames.addGame(gameName, null, ! canJoin);
    }

    @Override
    protected void handleNEWGAMEWITHOPTIONS(final SOCNewGameWithOptions mes)
    {
        String gameName = mes.getGameName();
        boolean canJoin = (mes.getMinVersion() <= Version.versionNumber());
        if (gameName.charAt(0) == SOCGames.MARKER_THIS_GAME_UNJOINABLE)
        {
            gameName = gameName.substring(1);
            canJoin = false;
        }
        serverGames.addGame(gameName, mes.getOptionsString(), ! canJoin);
    }

    @Override
    protected void handleJOINGAMEAUTH( SOCJoinGameAuth mes )
    {
        gotPassword = true;

        String gameName = mes.getGameName();
        SOCGameOptionSet opts = serverGames.parseGameOptions(gameName);

        final int bh = mes.getBoardHeight(), bw = mes.getBoardWidth();
        if ((bh != 0) || (bw != 0))
        {
            // Encode board size to pass through game constructor
            if (opts == null)
                opts = new SOCGameOptionSet();
            SOCGameOption opt = knownOpts.getKnownOption("_BHW", true);
            opt.setIntValue((bh << 8) | bw);
            opts.put(opt);
        }

        final SOCGame ga = new SOCGame(gameName, opts, knownOpts);
        ga.serverVersion = connection.getRemoteVersion();   // (isPractice) ? sLocalVersion : sVersion;
        games.put(gameName, ga);
    }
}
