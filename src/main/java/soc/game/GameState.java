package soc.game;

public enum GameState
{
    /**
     * Game states.  {@link #NEW_GAME} is a brand-new game, not yet ready to start playing.
     * Players are choosing where to sit, or have all sat but no one has yet clicked
     * the "start game" button. The board is empty sea, with no land hexes yet.
     * Next state from NEW_GAME is {@link #READY} if robots, or {@link #START1A} if only humans
     * are playing.
     *<P>
     * General assumptions for states and their numeric values:
     * <UL>
     * <LI> Any scenario-specific initial pieces, such as those in
     *      {@link SOCScenario#K_SC_PIRI SC_PIRI}, are sent while
     *      game state is still &lt; {@link #START1A}.
     * <LI> Active game states are >= {@link #START1A} and &lt; {@link #GAME_OVER}
     * <LI> Initial placement ends after {@link #START2B} or {@link #START3B}, going directly to {@link #ROLL_OR_CARD}
     * <LI> A Normal turn's "main phase" is {@link #PLAY1}, after dice-roll/card-play in {@link #ROLL_OR_CARD}
     * <LI> When the game is waiting for a player to react to something,
     *      state is > {@link #PLAY1}, &lt; {@link #GAME_OVER}; state name starts with
     *      PLACING_ or WAITING_
     * <LI> While reloading and resuming a saved game, state is {@link #LOADING}, barely &lt; {@link #GAME_OVER}.
     *      Some code may want to check state &lt; {@code LOADING} instead of &lt; {@code GAME_OVER}.
     * </UL>
     *<P>
     * The code reacts to (switches based on) game state in several places.
     * The main places to check, if you add a game state:
     *<UL>
     * <LI> {@link soc.client.SOCBoardPanel#updateMode()}
     * <LI> {@link soc.client.SOCBuildingPanel#updateButtonStatus()}
     * <LI> {@link soc.client.SOCPlayerInterface#updateAtGameState()}
     * <LI> {@link #putPiece(SOCPlayingPiece)}
     * <LI> {@link #advanceTurnStateAfterPutPiece()}
     * <LI> {@link #forceEndTurn()}
     * <LI> {@link soc.robot.SOCRobotBrain#run()}
     * <LI> {@link soc.server.SOCGameHandler#sendGameState(SOCGame)}
     * <LI> {@link soc.message.SOCGameState} javadoc list of states with related messages and client responses
     *</UL>
     * Also, if your state is similar to an existing state, do a where-used search
     * for that state, and decide where both states should be reacted to.
     *<P>
     * If your new state might be waiting for several players (not just the current player) to
     * respond with a choice (such as picking resources to discard or gain), also update
     * {@link soc.server.GameHandler#endTurnIfInactive(SOCGame, long)}.  Otherwise the robot will be
     * forced to lose its turn while waiting for human players.
     *<P>
     * Other places to check, if you add a game state:
     *<UL>
     * <LI> SOCBoardPanel.BoardPopupMenu.showBuild, showCancelBuild
     * <LI> SOCBoardPanel.drawBoard
     * <LI> SOCHandPanel.addPlayer, began, removePlayer, updateAtTurn, updateValue
     * <LI> SOCGame.addPlayer
     * <LI> SOCServerMessageHandler.handleSTARTGAME
     * <LI> Your game type's GameHandler.leaveGame, sitDown, GameMessageHandler.handleCANCELBUILDREQUEST, handlePUTPIECE
     * <LI> SOCPlayerClient.handleCANCELBUILDREQUEST, SOCDisplaylessPlayerClient.handleCANCELBUILDREQUEST
     *</UL>
     */
     NEW_GAME( 0 ),     // Brand new game, players sitting down

    /**
     * Ready to start playing.  All humans have chosen a seat.
     * Wait for requested robots to sit down.
     * Once robots have joined the game (this happens in other threads, possibly in other
     * processes), gameState will become {@link #START1A}.
     * @see #READY_RESET_WAIT_ROBOT_DISMISS
     * @see #LOADING_RESUMING
     */
    READY( 1 ),     // Ready to start playing

    /**
     * This game object has just been created by a reset, but the old game contains robot players,
     * so we must wait for them to leave before re-inviting anyone to continue the reset process.
     * Once they have all left, state becomes {@link #READY}.
     * See {@link #boardResetOngoingInfo} and (private) SOCServer.resetBoardAndNotify.
     * @since 1.1.07
     */
    READY_RESET_WAIT_ROBOT_DISMISS( 4 ),

    /**
     * Players place first settlement.  Proceed in order for each player; next state
     * is {@link #START1B} to place each player's 1st road.
     */
    START1A( 5),  // Players place 1st stlmt

    /**
     * Players place first road.  Next state is {@link #START1A} to place next
     * player's 1st settlement, or if all have placed settlements,
     * {@link #START2A} to place 2nd settlement.
     */
    START1B( 6), // Players place 1st road

    /**
     * Players place second settlement.  Proceed in reverse order for each player;
     * next state is {@link #START2B} to place 2nd road.
     * If the settlement is placed on a Gold Hex, the next state
     * is {@link #STARTS_WAITING_FOR_PICK_GOLD_RESOURCE}.
     *<P>
     * If game scenario option {@link SOCGameOptionSet#K_SC_3IP _SC_3IP} is set, then instead of
     * this second settlement giving resources, a third round of placement will do that;
     * next game state after START2A remains {@link #START2B}.
     */
    START2A( 10 ), // Players place 2nd stlmt

    /**
     * Players place second road.  Next state is {@link #START2A} to place previous
     * player's 2nd settlement (player changes in reverse order), or if all have placed
     * settlements, {@link #ROLL_OR_CARD} to begin first player's turn.
     *<P>
     * If game scenario option {@link SOCGameOptionSet#K_SC_3IP _SC_3IP} is set, then instead of
     * starting normal play, a third settlement and road are placed by each player,
     * with game state {@link #START3A}.
     */
    START2B( 11 ),	// Players place 2nd road

    /**
     * (Game scenarios) Players place third settlement.  Proceed in normal order
     * for each player; next state is {@link #START3B} to place 3rd road.
     * If the settlement is placed on a Gold Hex, the next state
     * is {@link #STARTS_WAITING_FOR_PICK_GOLD_RESOURCE}.
     *<P>
     * Valid only when game scenario option {@link SOCGameOptionSet#K_SC_3IP _SC_3IP} is set.
     */
    START3A( 12 ),  // Players place 3rd settlement

    /**
     * Players place third road.  Next state is {@link #START3A} to place previous
     * player's 3rd settlement (player changes in normal order), or if all have placed
     * settlements, {@link #ROLL_OR_CARD} to begin first player's turn.
     *<P>
     * Valid only when game scenario option {@link SOCGameOptionSet#K_SC_3IP _SC_3IP} is set.
     */
    START3B( 13 ),

    /**
     * Just placed an initial piece, waiting for current
     * player to choose which Gold Hex resources to receive.
     * This can happen after the second or third initial settlement,
     * or (with the fog scenario {@link SOCGameOptionSet#K_SC_FOG _SC_FOG})
     * when any initial road, settlement, or ship reveals a gold hex.
     *<P>
     * The next game state will be based on <tt>oldGameState</tt>,
     * which is the state whose placement led to {@link #STARTS_WAITING_FOR_PICK_GOLD_RESOURCE}.
     * For settlements not revealed from fog:
     * Next game state is {@link #START2B} to place 2nd road.
     * If game scenario option {@link SOCGameOptionSet#K_SC_3IP _SC_3IP} is set,
     * next game state can be {@link #START3B}.
     *<P>
     * Valid only when {@link #hasSeaBoard}, settlement adjacent to {@link SOCBoardLarge#GOLD_HEX},
     * or gold revealed from {@link SOCBoardLarge#FOG_HEX} by a placed road, ship, or settlement.
     *<P>
     * This is the highest-numbered possible starting state; value is {@link #ROLL_OR_CARD} - 1.
     *
     * @see #WAITING_FOR_PICK_GOLD_RESOURCE
     * @see #pickGoldHexResources(int, SOCResourceSet)
     * @since 2.0.00
     */
    STARTS_WAITING_FOR_PICK_GOLD_RESOURCE( 14 ),  // value must be 1 less than ROLL_OR_CARD

    /**
     * Start of a normal turn: Time to roll or play a card.
     * Next state depends on card or roll, but usually is {@link #PLAY1}.
     *<P>
     * If 7 is rolled, might be {@link #WAITING_FOR_DISCARDS} or {@link #WAITING_FOR_ROBBER_OR_PIRATE}
     *   or {@link #PLACING_ROBBER} or {@link #PLACING_PIRATE}.
     *<P>
     * If 7 is rolled with scenario option <tt>_SC_PIRI</tt>, there is no robber to move, but
     * the player will choose their robbery victim ({@link #WAITING_FOR_ROB_CHOOSE_PLAYER}) after any discards.
     *<P>
     * If the number rolled is on a gold hex, next state might be
     *   {@link #WAITING_FOR_PICK_GOLD_RESOURCE}.
     *<P>
     * <b>More special notes for scenario <tt>_SC_PIRI</tt>:</b> When the dice is rolled, the pirate fleet moves
     * along a path, and attacks the sole player with an adjacent settlement to the pirate hex, if any.
     * This is resolved before any of the normal dice-rolling actions (distributing resources, handling a 7, etc.)
     * If the player ties or loses (pirate fleet is stronger than player's fleet of warships), the roll is
     * handled as normal, as described above.  If the player wins, they get to pick a random resource.
     * Unless the roll is 7, this can be dealt with along with other gained resources (gold hexes).
     * So: <b>If the player wins and the roll is 7,</b> the player must pick their resource before any normal 7 discarding.
     * In that case only, the next state is {@link #WAITING_FOR_PICK_GOLD_RESOURCE}, which will be
     * followed by {@link #WAITING_FOR_DISCARDS} or {@link #WAITING_FOR_ROB_CHOOSE_PLAYER}.
     *<P>
     * Before v2.0.00 this state was named {@code PLAY}.
     */
    ROLL_OR_CARD( 15 ),	// Play continues normally; time to roll or play card

    /**
     * Dice were rolled, and results reported, but we're not quite ready to begin play yet.
     */
    DICE_ROLLED( 16 ),

    /**
     * Done rolling (or moving robber on 7).  Time for other turn actions,
     * such as building or buying or trading, or playing a card if not already done.
     * Next state depends on what's done, but usually is the next player's {@link #ROLL_OR_CARD}.
     */
    PLAY1( 20 ),	// Done rolling

    PLACING_ROAD( 30 ),
    PLACING_SETTLEMENT( 31 ),
    PLACING_CITY( 32 ),

    /**
     * Player is placing the robber on a new land hex.
     * May follow state {@link #WAITING_FOR_ROBBER_OR_PIRATE} if the game {@link #hasSeaBoard}.
     *<P>
     * Possible next game states:
     *<UL>
     * <LI> {@link #PLAY1}, after robbing no one or the single possible victim; from {@code oldGameState}
     * <LI> {@link #WAITING_FOR_ROB_CHOOSE_PLAYER} if multiple possible victims
     * <LI> In scenario {@link SOCGameOptionSet#K_SC_CLVI _SC_CLVI}, {@link #WAITING_FOR_ROB_CLOTH_OR_RESOURCE}
     *   if the victim has cloth and has resources
     * <LI> {@link #GAME_OVER}, if current player just won by gaining Largest Army
     *   (when there aren't multiple possible victims or another robber-related choice to make)
     *</UL>
     * @see #PLACING_PIRATE
     * @see #canMoveRobber(int, int)
     * @see #moveRobber(int, int)
     */
    PLACING_ROBBER( 33 ),

    /**
     * Player is placing the pirate ship on a new water hex,
     * in a game which {@link #hasSeaBoard}.
     * May follow state {@link #WAITING_FOR_ROBBER_OR_PIRATE}.
     * Has the same possible next game states as {@link #PLACING_ROBBER}.
     * @see #canMovePirate(int, int)
     * @see #movePirate(int, int)
     * @since 2.0.00
     */
    PLACING_PIRATE( 34 ),

    /**
     * This game {@link #hasSeaBoard}, and a player has bought and is placing a ship.
     * @since 2.0.00
     */
    PLACING_SHIP( 35 ),

    /**
     * Player is placing their first free road/ship.
     * If {@link #getCurrentDice()} == 0, the Road Building card was
     * played before rolling the dice.
     */
    PLACING_FREE_ROAD1( 40 ),

    /**
     * Player is placing their second free road/ship.
     * If {@link #getCurrentDice()} == 0, the Road Building card was
     * played before rolling the dice.
     */
    PLACING_FREE_ROAD2( 41 ),

    /**
     * Player is placing the special {@link SOCInventoryItem} held in {@link #getPlacingItem()}. For some kinds
     * of item, placement can sometimes be canceled by calling {@link #cancelPlaceInventoryItem(boolean)}.
     *<P>
     * The placement method depends on the scenario and item type; for example,
     * {@link SOCGameOptionSet#K_SC_FTRI _SC_FTRI} has trading port items and would
     * call {@link #placePort(int)}.
     *<P>
     * Placement requires its own game state (not {@code PLAY1}) because sometimes
     * it's triggered by the game after another action, not initiated by player request.
     *<P>
     * When setting this gameState: In case placement is canceled, set
     * {@code oldGameState} to {@link #PLAY1} or {@link #SPECIAL_BUILDING}.
     * @since 2.0.00
     */
    PLACING_INV_ITEM( 42 ),

    WAITING_FOR_DICE_RESULT_RESOURCES( 49 ),

    /**
     * Waiting for player(s) to discard, after 7 is rolled in {@link #rollDice()}.
     * Next game state is {@link #WAITING_FOR_DISCARDS}
     * (if other players still need to discard),
     * {@link #WAITING_FOR_ROBBER_OR_PIRATE},
     * or {@link #PLACING_ROBBER}.
     *<P>
     * In scenario option <tt>_SC_PIRI</tt>, there is no robber
     * to move, but the player will choose their robbery victim
     * ({@link #WAITING_FOR_ROB_CHOOSE_PLAYER}) after any discards.
     * If there are no possible victims, next state is {@link #PLAY1}.
     *
     * @see #discard(int, ResourceSet)
     */
    WAITING_FOR_DISCARDS( 50 ),

    /**
     * Waiting for player to choose a player to rob,
     * with the robber or pirate ship, after rolling 7 or
     * playing a Knight/Soldier card.
     * Next game state is {@link #PLAY1}, {@link #WAITING_FOR_ROB_CLOTH_OR_RESOURCE},
     * or {@link #GAME_OVER} if player just won by gaining Largest Army.
     *<P>
     * To see whether we're moving the robber or the pirate, use {@link #getRobberyPirateFlag()}.
     * To choose the player, call {@link #choosePlayerForRobbery(int)}.
     *<P>
     * In scenario option <tt>_SC_PIRI</tt>, there is no robber
     * to move, but the player will choose their robbery victim.
     * <tt>{@link #currentRoll}.sc_clvi_robPossibleVictims</tt>
     * holds the list of possible victims.  In that scenario,
     * the player also doesn't control the pirate ships, and
     * never has Knight cards to move the robber and steal.
     *<P>
     * So in that scenario, the only time the game state is {@code WAITING_FOR_ROB_CHOOSE_PLAYER}
     * is when the player must choose to steal from a possible victim, or choose to steal
     * from no one, after a 7 is rolled.  To choose the victim, call {@link #choosePlayerForRobbery(int)}.
     * To choose no one, call {@link #choosePlayerForRobbery(int) choosePlayerForRobbery(-1)}.
     *<P>
     * Before v2.0.00, this game state was called {@code WAITING_FOR_CHOICE}.
     *
     * @see #playKnight()
     * @see #canChoosePlayer(int)
     * @see #canChooseRobClothOrResource(int)
     * @see #stealFromPlayer(int, boolean)
     */
    WAITING_FOR_ROB_CHOOSE_PLAYER( 51 ),

    /**
     * Waiting for player to choose 2 resources (Discovery card)
     * Next game state is {@link #PLAY1}.
     */
    WAITING_FOR_DISCOVERY( 52 ),

    /**
     * Waiting for player to choose a resource (Monopoly card)
     * Next game state is {@link #PLAY1}.
     */
    WAITING_FOR_MONOPOLY( 53 ),

    /**
     * Waiting for player to choose the robber or the pirate ship,
     * after {@link #rollDice()} or {@link #playKnight()}.
     * Next game state is {@link #PLACING_ROBBER} or {@link #PLACING_PIRATE}.
     *<P>
     * Moving from {@code WAITING_FOR_ROBBER_OR_PIRATE} to those states preserves {@code oldGameState}.
     *
     * @see #canChooseMovePirate()
     * @see #chooseMovePirate(boolean)
     * @see #WAITING_FOR_DISCARDS
     * @since 2.0.00
     */
    WAITING_FOR_ROBBER_OR_PIRATE( 54 ),

    /**
     * Waiting for player to choose whether to rob cloth or rob a resource.
     * Previous game state is {@link #PLACING_PIRATE} or {@link #WAITING_FOR_ROB_CHOOSE_PLAYER}.
     * Next step: Call {@link #stealFromPlayer(int, boolean)} with result of that player's choice.
     *<P>
     * Next game state is {@link #PLAY1}, or {@link #GAME_OVER} if player just won by gaining Largest Army.
     *<P>
     * Used with scenario option {@link SOCGameOptionSet#K_SC_CLVI _SC_CLVI}.
     *
     * @see #movePirate(int, int)
     * @see #canChooseRobClothOrResource(int)
     * @since 2.0.00
     */
    WAITING_FOR_ROB_CLOTH_OR_RESOURCE( 55 ),

    /**
     * Waiting for player(s) to choose which Gold Hex resources to receive.
     * Next game state is usually {@link #PLAY1}, sometimes
     * {@link #PLACING_FREE_ROAD2} or {@link #SPECIAL_BUILDING}.
     * ({@link #oldGameState} holds the <b>next</b> state after this WAITING state.)
     *<P>
     * Valid only when {@link #hasSeaBoard}, settlements or cities
     * adjacent to {@link SOCBoardLarge#GOLD_HEX}.
     *<P>
     * When receiving this state from server, a client shouldn't immediately check their user player's
     * {@link SOCPlayer#getNeedToPickGoldHexResources()} or prompt the user to pick resources; those
     * players' clients will be sent a {@link soc.message.SOCSimpleRequest#PROMPT_PICK_RESOURCES} message shortly.
     *<P>
     * If scenario option {@link SOCGameOptionSet#K_SC_PIRI _SC_PIRI} is active,
     * this state is also used when a 7 is rolled and the player has won against a
     * pirate fleet attack.  They must choose a free resource.  {@link #oldGameState} is {@link #ROLL_OR_CARD}.
     * Then, the 7 is resolved as normal.  See {@link #ROLL_OR_CARD} javadoc for details.
     * That's the only time free resources are picked on rolling 7.
     *
     * @see #STARTS_WAITING_FOR_PICK_GOLD_RESOURCE
     * @see #pickGoldHexResources(int, SOCResourceSet)
     * @since 2.0.00
     */
    WAITING_FOR_PICK_GOLD_RESOURCE( 56 ),

    /**
     * The 6-player board's Special Building Phase.
     * Takes place at the end of any player's normal turn (roll, place, etc).
     * The Special Building Phase changes {@link #currentPlayerNumber}.
     * So, it begins by calling {@link #advanceTurn()} to
     * the next player, and continues clockwise until
     * {@link #currentPlayerNumber} == {@link #specialBuildPhase_afterPlayerNumber}.
     * At that point, the Special Building Phase is over,
     * and it's the next player's turn as usual.
     * @since 1.1.08
     */
    SPECIAL_BUILDING( 100 ),  // see advanceTurnToSpecialBuilding()

    /**
     * A saved game is being loaded. Its actual state is saved in field {@code oldGameState}.
     * Bots have joined to sit in seats which were bots in the saved game, and human seats
     * are currently unclaimed except for the requesting user if they were named as a player.
     * Before resuming play, server or user may need to satisfy conditions or constraints
     * (have a certain type of bot sit down at a given player number, etc).
     * If there are unclaimed non-vacant seats where bots will need to join,
     * next state is {@link #LOADING_RESUMING}, otherwise will resume at {@code oldGameState}.
     *<P>
     * This game state is higher-numbered than actively-playing states, slightly lower than {@link #GAME_OVER}
     * or {@link #LOADING_RESUMING}.
     *
     * @since 2.3.00
     */
    LOADING( 990 ),

    /**
     * A saved game was loaded and is about to resume, now waiting for some bots to rejoin.
     * Game's actual state is saved in field {@code oldGameState}.
     * Before we can resume play, server has requested some bots to fill unclaimed non-vacant seats,
     * which had humans when the game was saved. Server is waiting for the bots to join and sit down
     * (like state {@link #READY}). Once all bots have joined, gameState can resume at {@code oldGameState}.
     *<P>
     * This game state is higher-numbered than actively-playing states, slightly lower than {@link #GAME_OVER}
     * but higher than {@link #LOADING}.
     *
     * @since 2.3.00
     */
    LOADING_RESUMING( 992 ),

    /**
     * Convenience value for testing whether game state is GAME_OVER or greater.
     */
    ALMOST_OVER( 999 ),

    /**
     * The game is over.  A player has accumulated enough ({@link #vp_winner}) victory points,
     * or all players have left the game.
     * The winning player, if any, is {@link #getPlayerWithWin()}.
     * @see #checkForWinner()
     */
    GAME_OVER( 1000 ),	// The game is over

    /**
     * This game is an obsolete old copy of a new (reset) game with the same name.
     * To assist logic, numeric constant value is greater than {@link #GAME_OVER}.
     * @see #resetAsCopy()
     * @see #getOldGameState()
     * @since 1.1.00
     */
    RESET_OLD( 1001 ),

    INVALID( -1 );

    private final int intValue;

    public int getIntValue()
    {
        return intValue;
    }

    GameState( int value )
    {
        intValue = value;
    }

    public static GameState forInt( int code ) {
        for (GameState type : GameState.values())
        {
            if (type.getIntValue() == code) {
                return type;
            }
        }
        return INVALID;
    }

    public boolean lt( GameState other )
    {
        return intValue < other.getIntValue();
    }

    public boolean gt( GameState other )
    {
        return intValue > other.getIntValue();
    }
}
