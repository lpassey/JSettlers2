package soc.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import soc.game.SOCFortress;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCGameOptionSet;
import soc.game.SOCScenario;
import soc.game.SOCVersionedItem;
import soc.game.SOCVillage;
import soc.message.SOCMessage;

import java.util.HashMap;
import java.util.Map;

public class SOCServerScenario extends SOCScenario
{
    static
    {
        allScenarios = initAllScenarios();
    }

    /**
     * Get the scenario information about this known scenario.
     * Treat the returned value as read-only (is not cloned).
     * @param key  Scenario key name, such as {@link #K_SC_4ISL SC_4ISL}, from {@link #getAllKnownScenarios()}
     * @return information about a known scenario, or null if none with that key
     */
    public static SOCScenario getScenario(String key)
    {
        return allScenarios.get(key);  // null is ok
    }

    /**
     * Create a new known game scenario.
     *
     * @param key     Alphanumeric key name for this scenario;
     *                see {@link SOCVersionedItem#isAlphanumericUpcaseAscii(String)} for format.
     * @param minVers Minimum client version supporting this scenario, or -1.
     *                Same format as {@link soc.util.Version#versionNumber() Version.versionNumber()}.
     *                If not -1, {@code minVers} must be at least 2000 ({@link #VERSION_FOR_SCENARIOS}).
     *                To calculate the minimum version of a set of game options which might include a scenario,
     *                use {@link SOCVersionedItem#itemsMinimumVersion(Map) SOCVersionedItem.itemsMinimumVersion(opts)}.
     *                That calculation won't be done automatically by this constructor.
     * @param lastModVers Last-modified version for this scenario, or version which added it.
     *             This is the last change to the scenario itself as declared in {@link #getAllKnownScenarios()}:
     *             Ignore changes to {@code opts} last-modified versions, because changed option info
     *             is sent separately and automatically when the client connects.
     * @param desc    Descriptive brief text, to appear in the scenarios dialog.
     *             Desc must not contain {@link SOCMessage#sep_char} or {@link SOCMessage#sep2_char},
     *             and must evaluate true from {@link SOCMessage#isSingleLineAndSafe(String)}.
     * @param longDesc  Longer descriptive text, or null; see {@link #getLongDesc()} for requirements.
     * @param opts Scenario's {@link SOCGameOption}s, as a formatted string
     *             from {@link SOCGameOption#packOptionsToString(Map, boolean, boolean)}.
     *             Never "" or {@code null}.
     * @throws IllegalArgumentException if key length is > 8 or not alphanumeric,
     *        or if opts is {@code null} or the empty string "",
     *        or if desc contains {@link SOCMessage#sep_char} or {@link SOCMessage#sep2_char}
     *        or fail their described requirements,
     *        or if minVers or lastModVers is under 2000 but not -1
     */
    public SOCServerScenario( String key, int minVers, int lastModVers, String desc, String longDesc, String opts ) throws IllegalArgumentException
    {
        super( key, minVers, lastModVers, desc, longDesc, opts );
    }

    /**
     * Create a set of the known scenarios.
     * This method creates and returns a Map, but does not set the static {@link #allScenarios} field.
     * See {@link #getAllKnownScenarios()} for the current list of known scenarios.
     *
     * <h3>If you want to add a game scenario:</h3>
     *<UL>
     *<LI> Choose an unused key name: for example, {@code "SC_FOG"} for Fog Islands.
     *   The list of already-used key names is here within initAllScenarios().
     *<LI> Decide if all client versions can use your scenario.  Typically, if the scenario
     *   requires server changes but not any client changes, all clients can use it.
     *   (For example, the scenario doesn't add any new game options.)
     *   If the new scenario specifies any {@link #scOpts}, be sure the scenario's declared
     *   minimum version is >= those options' minimum versions; this won't be validated at runtime.
     *<LI> If your scenario requires new {@link SOCGameOption}s to change the rules or game behavior,
     *   create and test those; scenario game options all start with "_SC_".
     *   See {@link SOCGameOptionSet#getAllKnownOptions()} for details.
     *   If the new scenario has a new game option just for itself, instead of a reusable one like
     *   {@link SOCGameOptionSet#K_SC_SANY _SC_SANY}, the option name is "_" + scenario name:
     *   {@code "_SC_PIRI"} for scenario {@link #K_SC_PIRI SC_PIRI}.
     *<LI> If your scenario has special winning conditions, see {@link SOCGame#checkForWinner()}.
     *<LI> Rarely, a scenario changes the pirate or robber behavior.  If the new scenario does this,
     *   see {@link SOCGame#canChooseMovePirate()} or {@link SOCGame#rollDice()}.
     *   Currently no scenarios have a pirate but no robber, besides special case {@link #K_SC_PIRI SC_PIRI}
     *   where pirate fleet movement isn't controlled by the player. If adding a scenario with pirate but no robber,
     *   the client and robot would need changes to handle that; probably a {@link SOCGameOption} should be created
     *   for it and used in the scenario opts, client, and robot code.
     *<LI> Not all scenarios require a game option.  {@link #K_SC_TTD SC_TTD} has only a board layout,
     *   and doesn't change any game behavior from standard, so there is no {@code "_SC_TTD"} SOCGameOption.
     *<LI> Add the scenario's key to the list of "game scenario keynames"
     *   as a public static final String, such as {@link #K_SC_FOG}.
     *   Put a short description in the javadoc there and in {@link #getAllKnownScenarios()} javadoc's scenario list.
     *<LI> Create the scenario by calling {@code allSc.put} here in {@code initAllScenarios()}.
     *   Use the current version for the "last modified" field.
     *<LI> Create the board layout; see {@link soc.server.SOCBoardAtServer} javadoc.
     *<LI> Within {@link SOCGame}, don't change any code based on the scenario name;
     *   game behavior changes are based only on the {@link SOCGameOption}s implementing the scenario.
     *</UL>
     *
     * <h3>If you want to change a scenario (in a later version):</h3>
     *
     *   Typical changes to a game scenario would be:
     *<UL>
     *<LI> Change the {@link SOCVersionedItem#getDesc() description}
     *<LI> Change the {@link #getLongDesc() long description}
     *<LI> Change the {@link #scOpts options}
     *</UL>
     *   Things you can't change about a scenario, because inconsistencies would occur:
     *<UL>
     *<LI> {@link SOCVersionedItem#key name key}
     *<LI> {@link SOCVersionedItem#minVersion minVersion}
     *</UL>
     *
     *   <b>To make the change:</b>
     *<UL>
     *<LI> Change the scenario here in initAllScenarios; change the "last modified" field to
     *   the current game version. Otherwise the server can't tell the client what has
     *   changed about the scenario.
     *<LI> Search the entire source tree for its options' key names, to find places which may need an update.
     *<LI> Consider if any other places listed above (for add) need adjustment.
     *</UL>
     *
     * <h3>If you want to remove or obsolete a game scenario (in a later version):</h3>
     *
     * Please think twice beforehand; users may be surprised when something is missing, so this shouldn't
     * be done without a very good reason.  That said, the server is authoritative on scenarios.
     * If a scenario isn't in its known list ({@link #initAllScenarios()}), the client won't be
     * allowed to ask for it.  Any obsolete scenario should be kept around as commented-out code.
     * See {@link SOCGameOptionSet#getAllKnownOptions()} for things to think about when removing
     * game options used only in the obsolete scenario.
     *
     * @return a fresh copy of the "known" scenarios, with their hardcoded default values
     */
    public static Map<String, SOCScenario> initAllScenarios()
    {
        Map<String, SOCScenario> allSc = new HashMap<>();

        // Game scenarios, and their SOCGameOptions (rules and events)

        allSc.put(K_SC_NSHO, new SOCScenario
            (K_SC_NSHO, 2000, 2000,
                "New Shores",
                null,
                "_SC_SEAC=t,SBL=t,VP=t13"));

        allSc.put(K_SC_4ISL, new SOCScenario
            (K_SC_4ISL, 2000, 2000,
                "The Four Islands",
                "Start on one or two islands. Explore and gain SVP by building to others.",
                "_SC_SEAC=t,SBL=t,VP=t12"));

        allSc.put(K_SC_FOG, new SOCScenario
            (K_SC_FOG, 2000, 2000,
                "Fog Islands",
                "Some hexes are initially hidden by fog. When you build a ship or road to a foggy hex, that hex is revealed. "
                    + "Unless it's water, you are given its resource as a reward.",
                "_SC_FOG=t,SBL=t,VP=t12"));

        allSc.put(K_SC_TTD, new SOCScenario
            (K_SC_TTD, 2000, 2000,
                "Through The Desert",
                "Start on the main island. Explore and gain SVP by building to the small islands, or through the desert to the coast.",
                "_SC_SEAC=t,SBL=t,VP=t12"));

        allSc.put(K_SC_CLVI, new SOCScenario
            (K_SC_CLVI, 2000, 2000,
                "Cloth Trade with neutral villages",
                "The small islands' villages give you Cloth; every 2 cloth you have is 1 extra Victory Point. To gain cloth, "
                    + "build ships to a village. You can't move the pirate until you've reached a village. "
                    + "Each player to reach a village gets 1 of its cloth at that time, and 1 more "
                    + "whenever its number is rolled, until the village runs out. Pirate can steal cloth or resources. "
                    + "If fewer than 4 villages still have cloth, the game ends and the player "
                    + "with the most VP wins. (If tied, player with most cloth wins.)",
                "_SC_CLVI=t,SBL=t,VP=t14,_SC_3IP=t,_SC_0RVP=t"));

        allSc.put(K_SC_PIRI, new SOCScenario
            (K_SC_PIRI, 2000, 2000,
                "Pirate Islands and Fortresses",
                "A pirate fleet patrols, attacking to steal resources from weak players with adjacent settlements/cities until "
                    + "the player builds a strong fleet of Warships. Build ships directly to the "
                    + "Fortress of your color, which the pirates have captured from you. To win the game, you must reach the "
                    + "victory point goal and defeat the Fortress 3 times using warships. "
                    + "Ship routes can't branch out, only follow dotted lines to the Fortress. "
                    + "Strengthen your fleet by playing Warship development cards to upgrade your ships. "
                    + "When the pirate fleet attacks, you win if you have more Warships than the pirate fleet strength (randomly 1-6). "
                    + "No robber or largest army. When 7 is rolled, any pirate fleet attack happens before the usual discards.",
                "_SC_PIRI=t,SBL=t,VP=t10,_SC_0RVP=t"));  // win condition: 10 VP _and_ defeat a pirate fortress

        allSc.put(K_SC_FTRI, new SOCScenario
            (K_SC_FTRI, 2000, 2000,
                "The Forgotten Tribe",
                "Far areas of the board have small habitations of a \"forgotten tribe\" of settlers. "
                    + "When players build ships to reach them, they are greeted with \"gifts\" of a development card, "
                    + "Special Victory Point, or a Port given to the player which must be moved adjacent to one "
                    + "of their coastal settlements/cities if possible, or set aside to place later.",
                "_SC_FTRI=t,SBL=t,VP=t13"));

        allSc.put(K_SC_WOND, new SOCScenario
            (K_SC_WOND, 2000, 2000,
                "Wonders",
                "Each player chooses a unique Wonder and can build all 4 of its levels. "
                    + "Each Wonder has its own requirements before you may start it, such as "
                    + "several cities built or a port at a certain location. To win you "
                    + "must complete your Wonder's 4 levels, or reach 10 VP and complete "
                    + "more levels than any other player. Has no pirate ship.",
                "_SC_WOND=t,SBL=t,VP=t10,_SC_SANY=t"));  // win condition: Complete Wonder, or 10 VP _and_ built the most levels
        // The "all 4 levels" win condition is also stored in SOCSpecialItem.SC_WOND_WIN_LEVEL.

        // Uncomment to test scenario sync/negotiation between server and client versions.
        // Update the version numbers to current and current + 1.
        // Assumptions for testing:
        //   - Client and server are both current version (if current is v2.0.00, use 2000 here)
        //   - For testing, client or server version has been temporarily set to current + 1 (2001)
        // i18n/localization test reminder: resources/strings/server/toClient_*.properties:
        //   gamescen.SC_TSTNC.n = test-localizedname SC_TSTNC ...
        /*
        allSc.put("SC_TSTNC", new SOCScenario
            ("SC_TSTNC", 2000, 2001,
            "New: v+1 back-compat", null, "PLB=t,VP=t11,NT=y"));
        allSc.put("SC_TSTNO", new SOCScenario
            ("SC_TSTNO", 2001, 2001,
            "New: v+1 only", null, "PLB=t,VP=t15"));
         */

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setPrettyPrinting().disableHtmlEscaping().create();

        String json = gson.toJson( allSc );

        return allSc;

        // OBSOLETE SCENARIOS, REMOVED SCENARIOS - Move its allSc.put down here, commented out,
        //       including the version, date, and reason of the removal.
    }

    /**
     * Get all known scenario objects, mapping from their key names (such as {@link #K_SC_4ISL SC_4ISL}).
     *
     * <H3>Current Known Scenarios:</H3>
     *<UL>
     *<LI> {@link #K_SC_NSHO SC_NSHO}  New Shores
     *<LI> {@link #K_SC_4ISL SC_4ISL}  The Four Islands (Six on the 6-player board)
     *<LI> {@link #K_SC_FOG  SC_FOG}   Fog Islands
     *<LI> {@link #K_SC_TTD  SC_TTD}   Through The Desert
     *<LI> {@link #K_SC_CLVI SC_CLVI}  Cloth trade with neutral {@link SOCVillage villages}
     *<LI> {@link #K_SC_PIRI SC_PIRI}  Pirate Islands and {@link SOCFortress fortresses}
     *<LI> {@link #K_SC_FTRI SC_FTRI}  The Forgotten Tribe
     *<LI> {@link #K_SC_WOND SC_WOND}  Wonders
     *</UL>
     *  (See each scenario name field's javadoc for more details.)
     *
     * @return a deep copy of all known scenario objects
     * @see #getAllKnownScenarioKeynames()
     * @see #addKnownScenario(SOCScenario)
     * @see SOCGameOptionSet#getAllKnownOptions()
     */
    public static Map<String, SOCScenario> getAllKnownScenarios()
    {
        // To add a new scenario, see initAllScenarios().
        return cloneScenarios(allScenarios);
    }
}
