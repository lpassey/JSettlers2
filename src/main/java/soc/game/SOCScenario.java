/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2012-2021 Jeremy D Monin <jeremy@nand.net>
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
package soc.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import soc.message.SOCMessage;

/**
 * Scenarios for game rules and options on the {@link SOCBoardLarge large sea board}.
 *<P>
 * Scenarios use {@link SOCGameOption}s to change the game to the scenario's concept.
 * Each scenario's {@link #scOpts} field gives the scenario's option names and values.
 * The game also knows its scenario by setting {@link SOCGameOption} "SC" = {@link SOCVersionedItem#key key}.
 * Some scenarios restrict initial placement (see "land areas" in {@link SOCBoardLarge} class javadoc)
 * or have special winning conditions (see {@link SOCGame#checkForWinner()}).
 *<P>
 * Scenario name keys must start with a letter and contain only ASCII uppercase
 * letters ('A' through 'Z'), underscore ('_'), and digits ('0' through '9'), in order to normalize
 * handling and network message formats.  This is enforced in constructors via
 * {@link SOCGameOption#isAlphanumericUpcaseAscii(String)}.
 *<P>
 * For the same reason, descriptions must not contain
 * certain characters or span more than 1 line; this is checked by calling
 * {@link SOCMessage#isSingleLineAndSafe(String)} within constructors and setters.
 *<P>
 * <B>Version negotiation:</B><br>
 * Game options were introduced in 1.1.07; check server, client versions against
 * {@link soc.message.SOCNewGameWithOptions#VERSION_FOR_NEWGAMEWITHOPTIONS}.
 * Scenarios were introduced in 2.0.00, and negotiate the same way.
 * Each scenario has version information, because scenarios can be added or changed
 * with new versions of JSettlers.  Since games run on the server, the server is
 * authoritative about game scenarios and options:  If the client is newer, it must defer to the
 * server's older set of known scenarios and options.  At client connect, the client compares its
 * JSettlers version number to the server's, and asks for any changes if
 * their versions differ.
 *<P>
 * <B>I18N:</B><br>
 * Game scenario names and descriptions are also stored as {@code gamescen.*.n}, {@code .d}
 * in {@code server/strings/toClient_*.properties} to be sent to clients if needed.
 * At the client, scenario's text can be localized with {@link #setDesc(String, String)}.
 * See unit test {@link soctest.TestI18NGameoptScenStrings}.
 *<P>
 * @author Jeremy D. Monin &lt;jeremy@nand.net&gt;
 * @since 2.0.00
 */
public class SOCScenario extends SOCVersionedItem
    implements Cloneable, Comparable<Object>
{
    /** Version 2.0.00 (2000) introduced game scenarios. */
    public static final int VERSION_FOR_SCENARIOS = 2000;

    /**
     * Scenario key {@code SC_NSHO} for New Shores.
     * Board layout is based on the classic 4- or 6-player board, or a smaller 3-player main island, plus small
     * outlying islands. No main option or special rules, only the board layout and 2 SVP for reaching each island.
     */
    public static final String K_SC_NSHO = "SC_NSHO";

    /**
     * Scenario key {@code SC_4ISL} for The Four Islands.
     * No main option or special rules, only a board layout and SVP.
     */
    public static final String K_SC_4ISL = "SC_4ISL";

    /**
     * Scenario key {@code SC_FOG} for Fog Islands.
     * When a hex has been revealed from behind fog,
     * {@link SOCGameEvent#SGE_FOG_HEX_REVEALED} is fired.
     * Main option is {@link SOCGameOptionSet#K_SC_FOG}.
     */
    public static final String K_SC_FOG = "SC_FOG";

    /**
     * Scenario key {@code SC_TTD} for Through The Desert.
     * No main option or special rules, only a board layout and SVP.
     */
    public static final String K_SC_TTD = "SC_TTD";

    /**
     * Scenario key {@code SC_CLVI} for {@link SOCPlayerEvent#CLOTH_TRADE_ESTABLISHED_VILLAGE}:
     * Cloth Trade with neutral {@link SOCVillage villages}.
     * Main option is {@link SOCGameOptionSet#K_SC_CLVI}.
     *<P>
     * Game ends immediately if fewer than 4 villages still have cloth ({@link #SC_CLVI_VILLAGES_CLOTH_REMAINING_MIN}):
     * Winner is player with most VP, or most cloth if tied.
     *<P>
     * While starting a new game, the neutral villages are placed and sent to clients as part {@code "CV"}
     * of the board layout message while game state is still &lt; {@link SOCGame#START1A START1A}.
     */
    public static final String K_SC_CLVI = "SC_CLVI";

    /**
     * In scenario {@link #K_SC_CLVI SC_CLVI}, game ends immediately if
     * fewer than this many {@link SOCVillage villages} (4) still have cloth.
     * Per scenario rules, 4- and 6-player games use the same number here;
     * the 6-player layout has more villages and more players to reach them.
     */
    public static final int SC_CLVI_VILLAGES_CLOTH_REMAINING_MIN = 4;
        // If this value changes, must update scenario description text
        // and its translations (keys: gamescen.SC_CLVI.d, event.sc_clvi.game.ending.villages)

    /**
     * Scenario key {@code SC_PIRI} for Pirate Islands and {@link SOCFortress Fortresses}.
     * Main option is {@link SOCGameOptionSet#K_SC_PIRI}.
     *<P>
     * A pirate fleet circulates on a predefined path, stealing resources from weak players with
     * adjacent settlements/cities until the player upgrades their ships to warships.  To win,
     * the player must build ships directly to the Fortress with their color, and defeat it several
     * times using warships.  Also, ship routes can't branch in different directions in this scenario,
     * only extend from their ends.
     *<P>
     * Each player starts with an initial coastal settlement and ship. While starting a new game these
     * are placed and sent to clients while game state is still &lt; {@link SOCGame#START1A START1A}.
     *<P>
     * The pirate fleet moves with every dice roll, and battles whenever 1 player's settlement/city is
     * adjacent. See {@link SOCGame.RollResult#sc_piri_fleetAttackVictim} javadoc and fields linked there.
     * When a 7 is rolled, the fleet moves and any battle is resolved before the usual discards/robbery.
     * Players may choose to not rob from anyone on 7.
     *<P>
     * When a player defeats their Fortress, it's replaced by a {@link SOCSettlement}.
     */
    public static final String K_SC_PIRI = "SC_PIRI";

    /**
     * Scenario key {@code SC_FTRI} for the Forgotten Tribe.
     * Main option is {@link SOCGameOptionSet#K_SC_FTRI "_SC_FTRI"}.
     *<P>
     * Far areas of the board have small habitations of a "forgotten tribe" of settlers.
     * When players reach them (with a ship adjacent to various edge coordinates),
     * they are greeted with "gifts" of a development card or Special Victory Point.
     *<P>
     * Trade ports at these far areas can be claimed by players and must be moved adjacent to one
     * of their coastal settlements/cities, unless they have none that isn't at least separated 1 edge
     * from an existing port.  If that's the case, the claimed port is "set aside" for the
     * player to place later when they have such a coastal settlement.
     *<P>
     * When a player reaches a Special Edge and is awarded a gift, the game clears that edge's special
     * type, then fires a {@link SOCPlayerEvent#DEV_CARD_REACHED_SPECIAL_EDGE} or
     * {@link SOCPlayerEvent#SVP_REACHED_SPECIAL_EDGE} event.
     *<P>
     * When a player reaches a "gift" trade port, either the port is added to their inventory
     * as a {@link SOCInventoryItem} for later placement, or they must immediately place it:
     * {@link SOCGame#setPlacingItem(SOCInventoryItem)} is called, state becomes {@link SOCGame#PLACING_INV_ITEM}.
     */
    public static final String K_SC_FTRI = "SC_FTRI";

    /**
     * Scenario key {@code SC_WOND} for Wonders.
     * Main option is {@link SOCGameOptionSet#K_SC_WOND "_SC_WOND"}.
     * The pirate ship is not used in this scenario.
     *<P>
     * Players choose a unique Wonder and can build all 4 of its levels.
     * Each Wonder has its own requirements before they may start it,
     * such as several cities built or a port at a certain location.
     * Player must also use an unplaced {@link SOCShip} to start building a Wonder.
     *<P>
     * When a player starts to build a Wonder, it's added to their Special Items for visibility; see below.
     *<P>
     * To win the player must complete their Wonder's 4 levels, or reach 10 VP and
     * complete more levels than any other player.
     *<P>
     * Certain sets of nodes are special in this scenario's board layout.
     * Node sets are retrieved from {@link SOCBoardLarge#getAddedLayoutPart(String)} by key:
     *<UL>
     * <LI> {@code "N1"}: Desert Wasteland (for Great Wall wonder)
     * <LI> {@code "N2"}: Strait (for Great Bridge wonder)
     * <LI> {@code "N3"}: Adjacent to Strait ({@code N2}); initial placement not allowed here
     *     (this set is emptied after initial placement)
     *</UL>
     * This scenario also uses added layout part {@code "AL"} to specify that the nodes in {@code N1, N2,}
     * and {@code N3} become legal locations for settlements after initial placement.
     *<P>
     * The Wonders are stored as per-game Special Items: There are (1 + {@link SOCGame#maxPlayers}) wonders available,
     * held in game Special Item indexes 1 - <em>n</em>, with type key {@link SOCGameOptionSet#K_SC_WOND},
     * initialized in {@link SOCGame#updateAtBoardLayout()}.  When a player starts to build a Wonder, a reference
     * to its {@link SOCSpecialItem} is placed into index 0 of their Special Items:
     * {@link SOCPlayer#setSpecialItem(String, int, SOCSpecialItem) pl.setSpecialItem("_SC_WOND", 0, item)}.
     * Server will subtract 1 from player's available Ship count.
     *<P>
     * The player's request to build must use player item index (pi) 0, game item index (gi) 1 to <em>n</em>.
     * Completing all 4 levels of a Wonder ({@link SOCSpecialItem#SC_WOND_WIN_LEVEL}) wins the game.
     */
    public static final String K_SC_WOND = "SC_WOND";

    /**
     * Scenario's {@link SOCGameOption}s, as a formatted string
     * from {@link SOCGameOption#packOptionsToString(Map, boolean, boolean)}.
     * <em>Never {@code null} or empty</em>; {@code "-"} if scenario has no game options.
     */
    // TODO: change to {@link SOCGameOptionSet}
    public final String scOpts;

    /**
     * Detailed text for the scenario description and special rules; may be null.
     * See {@link #getLongDesc()} for more info and requirements.
     */
    private String scLongDesc;

    /**
     * Create a new unknown scenario ({@link SOCVersionedItem#isKnown isKnown} false).
     * Minimum version will be {@link Integer#MAX_VALUE}.
     * scDesc and scOpts will be an empty string.
     * @param key   Alphanumeric key name for this option;
     *                see {@link SOCVersionedItem#isAlphanumericUpcaseAscii(String)} for format.
     * @throws IllegalArgumentException if key length is > 8 or not alphanumeric
     */
    public SOCScenario( final String key )
        throws IllegalArgumentException
    {
        this(false, key, Integer.MAX_VALUE, 0, "", null, "");
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
    public SOCScenario( final String key, final int minVers, final int lastModVers,
         final String desc, final String longDesc, final String opts)
        throws IllegalArgumentException
    {
        this(true, key, minVers, lastModVers, desc, longDesc, opts);
    }

    /**
     * Create a new game scenario - common constructor.
     * @param isKnown True if scenario is known here ({@link SOCVersionedItem#isKnown isKnown} true)
     * @param key     Alphanumeric uppercase code for this scenario;
     *                see {@link SOCVersionedItem#isAlphanumericUpcaseAscii(String)} for format.
     *                Keys can be up to 8 characters long.
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
     * @param desc Descriptive brief text, to appear in the scenarios dialog.
     *             Desc must not contain {@link SOCMessage#sep_char} or {@link SOCMessage#sep2_char},
     *             and must evaluate true from {@link SOCMessage#isSingleLineAndSafe(String)}.
     * @param longDesc  Longer descriptive text, or null; see {@link #getLongDesc()} for requirements.
     * @param opts Scenario's {@link SOCGameOption}s, as a formatted string
     *             from {@link SOCGameOption#packOptionsToString(Map, boolean, boolean)}.
     *             Never "" or {@code null}.
     * @throws IllegalArgumentException  if key is not alphanumeric or length is > 8,
     *        or if desc contains {@link SOCMessage#sep_char} or {@link SOCMessage#sep2_char},
     *        or if opts is {@code null} or the empty string "",
     *        or if minVers or lastModVers is under 2000 but not -1
     */
    private SOCScenario( final boolean isKnown, final String key, final int minVers, final int lastModVers,
         final String desc, final String longDesc, final String opts )
        throws IllegalArgumentException
    {
        super(key, minVers, lastModVers, isKnown, desc);
            // checks isAlphanumericUpcaseAscii(key), isSingleLineAndSafe(desc)

        // validate & set scenario properties:
        if (key.length() > 8)
            throw new IllegalArgumentException("Key length > 8: " + key);
        if ((minVers < VERSION_FOR_SCENARIOS) && (minVers != -1))
            throw new IllegalArgumentException("minVers " + minVers + " for key " + key);
        if ((lastModVers < VERSION_FOR_SCENARIOS) && (lastModVers != -1))
            throw new IllegalArgumentException("lastModVers " + lastModVers + " for key " + key);
        if (longDesc != null)
        {
            if (! SOCMessage.isSingleLineAndSafe(longDesc, true))
                throw new IllegalArgumentException("longDesc fails isSingleLineAndSafe");
            if (longDesc.contains(SOCMessage.sep))
                throw new IllegalArgumentException("longDesc contains " + SOCMessage.sep);
        }
        if (opts == null)
            throw new IllegalArgumentException("opts null");
        if (opts.length() == 0)
            throw new IllegalArgumentException("opts empty");

        scOpts = opts;
        scLongDesc = longDesc;
    }

    /**
     * Clone this scenario map and its contents.
     * @param scens  a map of {@link SOCScenario}s, or null
     * @return a deep copy of all scenario objects within scens, or null if scens is null
     */
    public static Map<String, SOCScenario> cloneScenarios(final Map<String, SOCScenario> scens)
    {
        if (scens == null)
            return null;

        Map<String, SOCScenario> scens2 = new HashMap<>();
        for (Map.Entry<String, SOCScenario> e : scens.entrySet())
        {
            final SOCScenario sc = e.getValue();

            try
            {
                scens2.put(sc.key, (SOCScenario) sc.clone());
            }
            catch (CloneNotSupportedException ignored) {} // required, but not expected to happen
        }
        return scens2;
    }

    /**
     * Detailed text for the scenario description and special rules, or null.  Shown as a reminder at start of a game.
     * Must not contain network delimiter character {@link SOCMessage#sep_char}; {@link SOCMessage#sep2_char} is okay.
     * Must pass {@link SOCMessage#isSingleLineAndSafe(String, boolean) SOCMessage.isSingleLineAndSafe(String, true)}.
     * Don't include the description of any scenario game option, such as {@link SOCGameOptionSet#K_SC_SANY};
     * those will be taken from {@link SOCVersionedItem#getDesc() SOCGameOption.desc} and shown in the reminder message.
     *<P>
     * To update this field use {@link #setDesc(String, String)}.
     *
     * @return The long description, or null if none
     */
    public String getLongDesc()
    {
        return scLongDesc;
    }

    /**
     * For i18n, update the scenario's description text fields:
     * The name/short description ({@link #getDesc()}) and optional long description ({@link #getLongDesc()}).
     *
     * @param desc    Descriptive brief text, to appear in the scenarios dialog. Not null.
     *     Desc must not contain {@link SOCMessage#sep_char} or {@link SOCMessage#sep2_char},
     *     and must evaluate true from {@link SOCMessage#isSingleLineAndSafe(String)}.
     * @param longDesc  Longer descriptive text, or null; see {@link #getLongDesc()} for requirements.
     *     If null, keeps scenario's current (probably hardcoded unlocalized) longDesc.
     * @throws IllegalArgumentException if desc contains {@link SOCMessage#sep_char} or {@link SOCMessage#sep2_char},
     *        or desc or longDesc fails {@link SOCMessage#isSingleLineAndSafe(String, boolean)}
     * @see SOCVersionedItem#setDesc(String)
     */
    public void setDesc(final String desc, final String longDesc)
        throws IllegalArgumentException
    {
        if (longDesc != null)
        {
            if (! SOCMessage.isSingleLineAndSafe(longDesc, true))
                throw new IllegalArgumentException("longDesc fails isSingleLineAndSafe");
            if (longDesc.contains(SOCMessage.sep))
                throw new IllegalArgumentException("longDesc contains " + SOCMessage.sep);

            scLongDesc = longDesc;
        }

        setDesc(desc);  // checks isSingleLineAndSafe(desc)
    }

    /**
     * Get this scenario's description, for use in user-facing displays and GUI elements.
     * For a short unique identifier use {@link #key} instead.
     * @return {@link SOCVersionedItem#desc desc}
     */
    @Override
    public String toString()
    {
        return desc;
    }

    /**
     * Compare two scenarios, for display purposes. ({@link Comparable} interface)
     * Two game scenarios are considered equal if they have the same {@link SOCVersionedItem#key key}.
     * Greater/lesser is determined by
     * {@link SOCVersionedItem#getDesc() getDesc()}.{@link String#compareTo(String) compareTo()}.
     * @param other A SOCScenario to compare, or another object;  if other isn't a
     *              scenario, the {@link #hashCode()}s are compared.
     * @see #equals(Object)
     */
    public int compareTo(Object other)
    {
        if (other instanceof SOCScenario)
        {
            SOCScenario otherScenario = (SOCScenario) other;
            if (key.equals(otherScenario.key))
                return 0;
            return desc.compareTo(otherScenario.desc);
        }
        else
        {
            return hashCode() - other.hashCode();
        }
    }

    /**
     * Test if this scenario equals another object.
     * Two game scenarios are considered equal if they have the same {@link SOCVersionedItem#key key}.
     * @param other A SOCScenario to compare, or another object;  if other isn't a
     *      scenario, calls {@link Object#equals(Object) Object.equals(other)}.
     * @see #compareTo(Object)
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object other)
    {
        if (other == null)
            return false;
        else if (other instanceof SOCScenario)
        {
            SOCScenario otherScenario = (SOCScenario) other;
            // A Scenario object may have localized title and descriptions. The objects are equal if
            // the keys match and the option string matches.
            return (   key.equals( otherScenario.key )
                    && scOpts.equals( otherScenario.scOpts )
            );
        }
        else
            return super.equals(other);
    }

    /**
     * Return this scenario's hashCode for comparison purposes,
     * which is its {@link SOCVersionedItem#key key}'s {@link String#hashCode()}.
     * @see #equals(Object)
     */
    @Override
    public int hashCode() { return key.hashCode(); }

}
