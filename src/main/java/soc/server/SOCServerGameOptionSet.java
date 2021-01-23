package soc.server;

import soc.game.SOCBoardLarge;
import soc.game.SOCFortress;
import soc.game.SOCGame;
import soc.game.SOCGameEvent;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCPlayerEvent;
import soc.game.SOCScenario;
import soc.game.SOCVersionedItem;
import soc.game.SOCVillage;
import soc.server.savegame.SavedGameModel;
import soc.util.SOCFeatureSet;

import java.util.Map;

import static soc.game.SOCGameOption.FLAG_DROP_IF_UNUSED;

public class SOCServerGameOptionSet extends SOCGameOptionSet
{
    /**
     * Create and return a set of the Known Options.
     *<P>
     * Before v2.4.50 this method was {@code SOCGameOption.initAllOptions()}.
     *
     * <h3>Current known options:</h3>
     *<UL>
     *<LI> PL  Maximum # players (2-6) - default 4
     *<LI> PLB Use 6-player board* - default false
     *<LI> PLP 6-player board: Can Special Build only if 5 or 6 players in game* - default false
     *<LI> SBL Use sea board layout (has a large, varying max size) - default false
     *<LI> RD  Robber can't return to the desert - default false
     *<LI> N7  Roll no 7s during first # rounds - default false, 7 rounds
     *<LI> N7C Roll no 7s until a city is built - default false
     *<LI> BC  Break up clumps of # or more same-type ports/hexes - default 4
     *<LI> NT  No trading allowed - default false
     *<LI> VP  Victory points (10-15) - default 10
     *<LI> _SC  Game Scenario (optional groups of rules; see {@link SOCScenario})
     *<LI> _BHW  Board height and width, if not default, for {@link SOCBoardLarge}: 0xRRCC.
     *           Used only at client, for board size received in JoinGame message from server
     *           to pass through SOCGame constructor into SOCBoard factory
     *</UL>
     *  * Grouping: PLB, PLP are 3 characters, not 2, and the first 2 characters match an
     *    existing option. So in NewGameOptionsFrame, they appear on the lines following
     *    the PL option in client version 1.1.13 and above.<br />
     *<br />
     * <h3>Current Game Scenario options:</h3>
     *<UL>
     *<LI> {@link #K_SC_SANY _SC_SANY}  SVP to settle in any new land area:
     *         {@link SOCPlayerEvent#SVP_SETTLED_ANY_NEW_LANDAREA}
     *<LI> {@link #K_SC_SEAC _SC_SEAC}  2 SVP each time settle in another new land area:
     *         {@link SOCPlayerEvent#SVP_SETTLED_EACH_NEW_LANDAREA}
     *<LI> {@link #K_SC_FOG  _SC_FOG}   A hex has been revealed from behind fog:
     *         {@link SOCGameEvent#SGE_FOG_HEX_REVEALED}: See {@link SOCScenario#K_SC_FOG}
     *<LI> {@link #K_SC_0RVP _SC_0RVP}  No VP for longest road / longest trade route
     *<LI> {@link #K_SC_3IP  _SC_3IP}   Third initial settlement and road/ship placement
     *<LI> {@link #K_SC_CLVI _SC_CLVI}  Cloth trade with neutral {@link SOCVillage villages}: See {@link SOCScenario#K_SC_CLVI}
     *<LI> {@link #K_SC_FTRI _SC_FTRI}  The Forgotten Tribe: See {@link SOCScenario#K_SC_FTRI}
     *<LI> {@link #K_SC_PIRI _SC_PIRI}  Pirate Islands and {@link SOCFortress fortresses}: See {@link SOCScenario#K_SC_PIRI}
     *<LI> {@link #K_SC_WOND _SC_WOND}  Wonders: See {@link SOCScenario#K_SC_WOND}
     *</UL>
     *
     * <h3>Options for quick tests/prototyping:</h3>
     *
     * For quick tests or prototyping, including third-party bot/AI/client development,
     * there are a few predefined but unused game options available:<br />
     *<UL>
     *<LI> {@link #K__EXT_BOT _EXT_BOT}  Extra option for robot development
     *<LI> {@link #K__EXT_CLI _EXT_CLI}  Extra option for client development
     *<LI> {@link #K__EXT_GAM _EXT_GAM}  Extra option for game development
     *</UL>
     * These can be used to easily send config or debug settings to your bot or client when it joins a game,
     * by setting a default value at the server's command line or properties file.<br />
     * <br />
     * <h3>If you want to add a game option:</h3>
     *<UL>
     *<LI> Choose an unused unique name key: for example, "PL" for "max players".
     *   All in-game code uses these key strings to query and change
     *   game option settings; only a very few places use SOCGameOption
     *   objects, and you won't need to adjust those places.
     *   The list of already-used key names is here within getAllKnownOptions.
     *   <P>
     *   If your option is useful only for developers or in other special situations,
     *   and should normally be hidden from clients, define it as an Inactive Option
     *   by using the {@link SOCGameOption#FLAG_INACTIVE_HIDDEN} flag.
     *   <P>
     *   If you're forking JSettlers or developing a third-party client, server, or bot,
     *   any game options you add should use {@code '3'} as the second character of
     *   their name key: {@code "_3"}, {@code "T3"}, etc.
     *   Use {@link SOCGameOption#FLAG_3RD_PARTY} when specifying your options; see its javadoc for details.
     *   <P>
     *   If your option supports a {@link SOCScenario}, its name should
     *   start with "_SC_" and it should have a constant name field here
     *   (like {@link #K_SC_3IP}) with a short descriptive javadoc.
     *   Link in javadoc to the SOCScenario field and add it to the list above.
     *   Because name starts with "_SC_", constructor will automatically call
     *   {@link SOCGameOption#setClientFeature(String) setClientFeature}({@link SOCFeatureSet#CLIENT_SEA_BOARD}).
     *   If you need a different client feature instead, or none, call that setter afterwards.
     *<LI> Decide which {@link SOCGameOption#optType option type} your option will be
     *   (boolean, enumerated, int+bool, etc.), and its default value.
     *   Typically the default will let the game behave as it did before
     *   the option existed (for example, the "max players" default is 4).
     *   Its default value on your own server can be changed at runtime.
     *<LI> Decide if all client versions can use your option.  Typically, if the option
     *   requires server changes but not any client changes, all clients can use it.
     *   (For example, "N7" for "roll no 7s early in the game" works with any client
     *   because dice rolls are done at the server.)
     *<LI> Create the option by calling opt.put here in getAllKnownOptions.
     *   Use the current version for the "last modified" field.
     *<LI> Add the new option's description to the {@code gameopt.*} section of
     *   {@code server/strings/toClient_*.properties} to be sent to clients if needed.
     *<LI> If only <em>some values</em> of the option will require client changes,
     *   also update {@link SOCGameOption#getMinVersion(Map)}.  (For example, if "PL"'s value is 5 or 6,
     *   a new client would be needed to display that many players at once, but 2 - 4
     *   can use any client version.) <BR>
     *   If this is the case and your option type
     *   is {@link SOCGameOption#OTYPE_ENUM} or {@link SOCGameOption#OTYPE_ENUMBOOL}, also update
     *   {@link SOCGameOption#getMaxEnumValueForVersion(String, int)}.
     *   Otherwise, update {@link #getMaxIntValueForVersion(String, int)}.
     *<LI> If the new option can be used by old clients by changing the values of
     *   <em>other</em> related options when game options are sent to those versions,
     *   add code to {@link SOCGameOption#getMinVersion(Map)}. <BR>
     *   For example, the boolean "PLB" can force use of the 6-player board in
     *   versions 1.1.08 - 1.1.12 by changing "PL"'s value to 5 or 6.
     *<LI> Within {@link SOCGame}, don't add any object fields due to the new option;
     *   instead call {@link SOCGame#isGameOptionDefined(String)},
     *   {@link SOCGame#getGameOptionIntValue(String)}, etc.
     *   Look for game methods where game behavior changes with the new option,
     *   and adjust those.
     *<LI> Check the server and clients for places which must check for the new option.
     *   Typically these will be the <strong>places which call the game methods</strong> affected.
     *   <UL>
     *   <LI> {@link soc.server.SOCServer} is the server class,
     *           see its "handle" methods for network messages
     *   <LI> {@link soc.client.SOCPlayerClient} is the graphical client
     *   <LI> {@link soc.robot.SOCRobotClient} and {@link soc.robot.SOCRobotBrain#run()}
     *           together handle the robot client messages
     *   <LI> {@link soc.baseclient.SOCDisplaylessPlayerClient} is the foundation for the robot client,
     *           and handles some of its messages
     *   </UL>
     *   Some options don't need any code at the robot; for example, the robot doesn't
     *   care about the maximum number of players in a game, because the server tells the
     *   robot when to join a game.
     *   <P>
     *   Some options need code only in the {@link SOCGame} constructor.
     *<LI> To find other places which may possibly need an update from your new option,
     *   search the entire source tree for this marker: <code> // NEW_OPTION</code>
     *   <br>
     *   This would include places like
     *   {@link soc.util.SOCRobotParameters#copyIfOptionChanged(SOCGameOptionSet)}
     *   which ignore most, but not all, game options.
     *<LI> If the new option adds new game/player/board fields or piece types which aren't
     *   currently in {@link SavedGameModel}:
     *   <UL>
     *   <LI> Either add the fields there, and test to make sure SAVEGAME/LOADGAME handles their data properly
     *   <LI> Or, check for and reject the new option in {@link SavedGameModel#checkCanSave(SOCGame)}
     *       and {@link SavedGameModel#checkCanLoad(SOCGameOptionSet)};
     *       add a {@code TODO} to later add support to SavedGameModel
     *   </UL>
     *</UL>
     *
     *<h3>If you want to change a game option (in a later version):</h3>
     *
     *   Typical changes to a game option would be:
     *<UL>
     *<LI> Add new values to an {@link SOCGameOption#OTYPE_ENUM} enumerated option;
     *   they must be added to the end of the list
     *<LI> Change the maximum or minimum permitted values for an
     *   {@link SOCGameOption#OTYPE_INT} integer option
     *<LI> Change the default value, although this can also be done
     *   at runtime on the command line
     *<LI> Change the value at the server based on other options' values
     *</UL>
     *   Things you can't change about an option, because inconsistencies would occur:
     *<UL>
     *<LI> {@link SOCVersionedItem#key name key}
     *<LI> {@link SOCGameOption#optType}
     *<LI> {@link SOCGameOption#minVersion}
     *<LI> {@link SOCGameOption#optFlags} such as {@link SOCGameOption#FLAG_DROP_IF_UNUSED}:
     *     Newly defined flags could maybe be added, if old versions can safely ignore them,
     *     but a flag can't be removed from an option in a later version.
     *<LI> For {@link SOCGameOption#OTYPE_ENUM} and {@link SOCGameOption#OTYPE_ENUMBOOL}, you can't remove options
     *     or change the meaning of current ones, because this would mean that the option's intValue (sent over
     *     the network) would mean different things to different-versioned clients in the game.
     *</UL>
     *
     *<H4>To make the change:</H4>
     *<UL>
     *<LI> Change the option here in getAllKnownOptions; change the "last modified" field to
     *   the current game version. Otherwise the server can't tell the client what has
     *   changed about the option.
     *<LI> If new values require a newer minimum client version, add code to {@link SOCGameOption#getMinVersion(Map)}.
     *<LI> If adding a new enum value for {@link SOCGameOption#OTYPE_ENUM} and {@link SOCGameOption#OTYPE_ENUMBOOL},
     *   add code to {@link SOCGameOption#getMaxEnumValueForVersion(String, int)}.
     *<LI> If increasing the maximum value of an int-valued parameter, and the new maximum
     *   requires a certain version, add code to {@link SOCGameOption#getMaxIntValueForVersion(String, int)}.
     *   For example, versions below 1.1.08 limit "max players" to 4.
     *<LI> Search the entire source tree for its key name, to find places which may need an update.
     *<LI> Consider if any other places listed above (for add) need adjustment.
     *</UL>
     *
     * <h3>If you want to remove or obsolete a game option (in a later version):</h3>
     *
     * Please think twice beforehand; breaking compatibility with older clients shouldn't
     * be done without a very good reason.  That said, the server is authoritative on options.
     * If an option isn't in its Known Options set (from when it called this {@code getAllKnownOptions()} method),
     * the client won't be allowed to ask for it.  Any obsolete options should be kept around as commented-out code.
     *
     * @return a fresh copy of the "known" options, with their hardcoded default values.
     *     Includes all defined options, including those with {@link SOCGameOption#FLAG_INACTIVE_HIDDEN}
     *     or {@link SOCGameOption#FLAG_3RD_PARTY}.
     * @see #getKnownOption(String, boolean)
     * @see #addKnownOption(SOCGameOption)
     * @see SOCScenario#getAllKnownScenarios()
     * @since 1.1.07
     */
    public static SOCGameOptionSet getAllKnownOptions()
    {
        final SOCGameOptionSet opts = new SOCGameOptionSet();

        // I18N: Game option descriptions are also stored as gameopt.* in server/strings/toClient_*.properties
        //       to be sent to clients if needed.

        final SOCGameOption optPL = new SOCGameOption(
            "PL", -1, 1108, 4, 2, 6, 0,
            "Maximum # players" );
        opts.add( optPL );

        final SOCGameOption optPLB = new SOCGameOption(
            "PLB", 1108, 1113, false, FLAG_DROP_IF_UNUSED,
            "Use 6-player board" );
        optPLB.setClientFeature( SOCFeatureSet.CLIENT_6_PLAYERS );
        opts.add( optPLB );

        final SOCGameOption optPLP = new SOCGameOption(
            "PLP", 1108, 2300, false, FLAG_DROP_IF_UNUSED,
            "6-player board: Can Special Build only if 5 or 6 players in game" );
        optPLP.setClientFeature( SOCFeatureSet.CLIENT_6_PLAYERS );
        opts.add( optPLP );

        SOCGameOption optSBL = new SOCGameOption(
            "SBL", 2000, 2000, false, FLAG_DROP_IF_UNUSED,
            "Use sea board" );  // see also SOCBoardLarge
        optSBL.setClientFeature( SOCFeatureSet.CLIENT_SEA_BOARD );
        opts.add( optSBL );

        opts.add( new SOCGameOption
            ( "_BHW", 2000, 2000, 0, 0, 0xFFFF, FLAG_DROP_IF_UNUSED | SOCGameOption.FLAG_INTERNAL_GAME_PROPERTY,
                "Large board's height and width (0xRRCC) if not default (local to client only)" ) );
        opts.add( new SOCGameOption
            ( "RD", -1, 1107, false, 0, "Robber can't return to the desert" ) );
        opts.add( new SOCGameOption
            ( "N7", -1, 1107, false, 7, 1, 999, 0, "Roll no 7s during first # rounds" ) );
        // N7C's keyname puts it after N7 in the NewGameOptionsFrame list
        opts.add( new SOCGameOption
            ( "N7C", -1, 1119, false, FLAG_DROP_IF_UNUSED, "Roll no 7s until a city is built" ) );
        opts.add( new SOCGameOption
            ( "BC", -1, 1107, true, 4, 3, 9, 0, "Break up clumps of # or more same-type hexes/ports" ) );
        opts.add( new SOCGameOption
            ( "NT", 1107, 1107, false, FLAG_DROP_IF_UNUSED, "No trading allowed between players" ) );
        opts.add( new SOCGameOption
            ( "VP", -1, 2000, false, 10, 10, 20, FLAG_DROP_IF_UNUSED, "Victory points to win: #" ) );
        // If min or max changes, test client to make sure New Game dialog still shows it as a dropdown
        // (not a text box) for user convenience

        final SOCGameOption optSC = new SOCGameOption
            ( "SC", 2000, 2000, 8, false, FLAG_DROP_IF_UNUSED, "Game Scenario: #" );
        optSC.setClientFeature( SOCFeatureSet.CLIENT_SCENARIO_VERSION );
        opts.add( optSC );

        // Game scenario options (rules and events)
        //      Constructor calls setClientFeature(SOCFeatureSet.CLIENT_SCENARIO_VERSION) for these
        //      because keyname.startsWith("_SC_")

        //      I18N note: NewGameOptionsFrame.showScenarioInfoDialog() assumes these
        //      all start with the text "Scenarios:". When localizing, be sure to
        //      keep a consistent prefix that showScenarioInfoDialog() knows to look for.
        //      In client/strings/data_*.properties, set game.options.scenario.optprefix to that prefix.

        opts.add( new SOCGameOption
            ( K_SC_SANY, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: SVP for your first settlement on any island after initial placement" ) );
        opts.add( new SOCGameOption
            ( K_SC_SEAC, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: 2 SVP for your first settlement on each island after initial placement" ) );
        opts.add( new SOCGameOption
            ( K_SC_FOG, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: Some hexes initially hidden by fog" ) );
        opts.add( new SOCGameOption
            ( K_SC_0RVP, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: No longest trade route VP (no Longest Road)" ) );
        opts.add( new SOCGameOption
            ( K_SC_3IP, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: Third initial settlement" ) );
        opts.add( new SOCGameOption
            ( K_SC_CLVI, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: Cloth Trade with neutral villages" ) );
        opts.add( new SOCGameOption
            ( K_SC_PIRI, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: Pirate Islands and fortresses" ) );
        opts.add( new SOCGameOption
            ( K_SC_FTRI, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: The Forgotten Tribe" ) );
        opts.add( new SOCGameOption
            ( K_SC_WOND, 2000, 2000, false, FLAG_DROP_IF_UNUSED,
                "Scenarios: Wonders" ) );

        // "Extra" options for third-party developers

        opts.add( new SOCGameOption
            ( K__EXT_BOT, 2000, 2000, SOCGameOption.TEXT_OPTION_MAX_LENGTH, false, FLAG_DROP_IF_UNUSED,
                "Extra non-core option available for robots in this game" ) );
        opts.add( new SOCGameOption
            ( K__EXT_CLI, 2000, 2000, SOCGameOption.TEXT_OPTION_MAX_LENGTH, false, FLAG_DROP_IF_UNUSED,
                "Extra non-core option available for clients in this game" ) );
        opts.add( new SOCGameOption
            ( K__EXT_GAM, 2000, 2000, SOCGameOption.TEXT_OPTION_MAX_LENGTH, false, FLAG_DROP_IF_UNUSED,
                "Extra non-core option available for this game" ) );

        // Player info observability, for developers

        opts.add( new SOCGameOption
            ( K_PLAY_FO, 2000, 2450, false, SOCGameOption.FLAG_INACTIVE_HIDDEN | FLAG_DROP_IF_UNUSED,
                "Show all player info and resources" ) );
        opts.add( new SOCGameOption
            ( K_PLAY_VPO, 2000, 2450, false, SOCGameOption.FLAG_INACTIVE_HIDDEN | FLAG_DROP_IF_UNUSED,
                "Show all VP/dev card info" ) );

        // NEW_OPTION - Add opt.put here at end of list, and update the
        //       list of "current known options" in javadoc just above.

        /*
            // A commented-out debug option for testing convenience:
            // Un-comment to let client create games that no one can join.

        opts.add(new SOCGameOption
            ("DEBUGNOJOIN", Integer.MAX_VALUE, Integer.MAX_VALUE, false,
             SOCGameOption.FLAG_DROP_IF_UNUSED, "Cannot join this game"));
        */

        /*
            // A commented-out debug option is kept here for each option type's testing convenience.
            // OTYPE_* - Add a commented-out debug of the new type, for testing the new type.

        opts.add(new SOCGameOption
            ("DEBUGBOOL", 2000, Version.versionNumber(), false, SOCGameOption.FLAG_DROP_IF_UNUSED, "Test option bool"));
        opts.add(new SOCGameOption
            ("DEBUGENUM", 1107, Version.versionNumber(), 3,
             new String[]{ "First", "Second", "Third", "Fourth"},
             SOCGameOption.FLAG_DROP_IF_UNUSED, "Test option # enum"));
        opts.add(new SOCGameOption
            ("DEBUGENUMBOOL", 1107, Version.versionNumber(), true, 3,
             new String[]{ "First", "Second", "Third", "Fourth"},
             SOCGameOption.FLAG_DROP_IF_UNUSED, "Test option # enumbool"));
        opts.add(new SOCGameOption
            ("DEBUGINT", -1, Version.versionNumber(), 500, 1, 1000,
             SOCGameOption.FLAG_DROP_IF_UNUSED, "Test option int # (range 1-1000)"));
        opts.add(new SOCGameOption
            ("DEBUGSTR", 1107, Version.versionNumber(), 20, false, SOCGameOption.FLAG_DROP_IF_UNUSED, "Test option str"));
        opts.add(new SOCGameOption
            ("DEBUGSTRHIDE", 1107, Version.versionNumber(), 20, true, SOCGameOption.FLAG_DROP_IF_UNUSED, "Test option strhide"));
        */

        return opts;

        /*
            // TEST CODE: simple callback for each option, that just echoes old/new value

        SOCGameOption.ChangeListener testCL = new SOCGameOption.ChangeListener()
        {
            public void valueChanged
                (final SOCGameOption opt, Object oldValue, Object newValue,
                 final SOCGameOptionSet currentOpts, final SOCGameOptionSet knownOpts)
            {
                System.err.println("Test ChangeListener: " + opt.key
                    + " changed from " + oldValue + " to " + newValue);
            }
        };
        for (SOCGameOption op : opts)
        {
            if (! op.hasChangeListener())
                op.addChangeListener(testCL);
        }

            // END TEST CODE
        */

        // OBSOLETE OPTIONS, REMOVED OPTIONS - Move its opt.put down here, commented out,
        //       including the version, date, and reason of the removal.
    }
}
