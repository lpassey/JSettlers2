/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2009,2010,2014,2017-2019 Jeremy D Monin <jeremy@nand.net>
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
package soc.message;

import java.util.StringTokenizer;


/**
 * This message contains the scores for the people at a game.
 * Used at end of game to display true scores (totals including points from VP cards).
 *<P>
 * Any game-information messages which reveal hidden state are sent
 * before, not after, this message. When client receives this
 * message, take it as a signal to reveal true scores and maybe
 * show/announce other interesting information such as VP dev cards.
 *
 *<H3>Message sequence:</H3>
 *<UL>
 *<LI> {@link SOCGameState}({@link soc.game.SOCGame#OVER OVER})
 *<LI> Any other messages revealing hidden information about game's details
 *<LI> This message
 *</UL>
 *
 * @author Robert S. Thomas
 */
public class SOCGameStats extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The player scores by seat number; always indexed 0 to {@link soc.game.SOCGame#maxPlayers} - 1,
     *   regardless of number of players in the game.
     */
    private int[] scores;

    /**
     * Where robots are sitting; indexed same as scores. For example, robots[0] is true if a robot
     * is sitting in seat 0, robots[1] is true if a robot is sitting in seat 1, robots[2] is false
     * is /not/ sitting in seat 2, and so forth.
     */
    private boolean[] robots;

    /**
     * Create a GameStats message
     *
     * @param ga  the name of the game
     * @param sc  the scores; always indexed 0 to
     *   {@link soc.game.SOCGame#maxPlayers} - 1,
     *   regardless of number of players in the game
     * @param rb  where robots are sitting; indexed same as scores
     */
    public SOCGameStats(String ga, int[] sc, boolean[] rb)
    {
        super( GAMESTATS, ga );
        scores = sc;
        robots = rb;
    }

    /**
     * @return the scores
     */
    public int[] getScores()
    {
        return scores;
    }

    /**
     * @return if a robot is sitting in a particular seat. robots[0] is true if a robot is sitting
     * in seat 0, robots[1] is true if a robot is sitting in seat 1, robots[2] is false if a robot
     * is /not/ sitting in seat 2, and so forth.
     */
    public boolean[] getRobotSeats()
    {
        return robots;
    }

    /**
     * @return the command string
     */
    public String toCmd()
    {
        StringBuilder cmd = new StringBuilder( super.toCmd() );

        for (int value : scores)
        {
            cmd.append( sep2 ).append( value );
        }
        for (boolean b : robots)
        {
            cmd.append( sep2 ).append( b );
        }
        return cmd.toString();
    }

    /**
     * Parse the command String into a GameStats message
     *
     * @param s   the String to parse
     * @return    a GameStats message, or null if the data is garbled
     */
    public static SOCGameStats parseDataStr(String s)
    {
        String ga; // the game name
        int[] sc; // the scores
        boolean[] rb; // where robots are sitting

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            final int maxPlayers = st.countTokens() / 2;
            sc = new int[maxPlayers];
            rb = new boolean[maxPlayers];

            for (int i = 0; i < maxPlayers; i++)
            {
                sc[i] = Integer.parseInt(st.nextToken());
            }

            for (int i = 0; i < maxPlayers; i++)
            {
                rb[i] = Boolean.valueOf( st.nextToken() );
            }
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCGameStats(ga, sc, rb);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        StringBuilder text = new StringBuilder("SOCGameStats:game=");
        text.append( getGame() );
        for (int score : scores )
        {
            text.append( "|" );
            text.append( score );
        }

        for (boolean robot : robots)
        {
            text.append( "|" );
            text.append( robot );
        }

        return text.toString();
    }
}
