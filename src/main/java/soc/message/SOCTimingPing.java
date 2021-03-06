/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2011,2013,2015,2017 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2003-2004  Robert S. Thomas
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
 **/
package soc.message;


/**
 * This message contains a timing ping that the bot client sends locally to its active games'
 * inbound message queues, once each second. Used by {@code SOCRobotBrain} for timing.
 *<P>
 * Before v1.1.13, the timing ping was {@link SOCGameTextMsg}({@code "*PING*"}).
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @see SOCGameServerText
 * @see SOCServerPing
 * @since 1.1.13
 */
public class SOCTimingPing extends SOCMessageForGame
{
    /** matches version (1.1.13) */
    private static final long serialVersionUID = 1113L;

    /**
     * Create a SOCTimingPing message.
     *
     * @param gameName  name of game
     */
    public SOCTimingPing( String gameName )
    {
        super( TIMINGPING, gameName );
    }

    /**
     * Parse the command String into a TIMINGPING message.
     *
     * @param s   the String to parse; should contain only the game name
     * @return    a TIMINGPING message, or null if the data is garbled
     */
    public static SOCTimingPing parseDataStr(String s)
    {
        return new SOCTimingPing(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCTimingPing:game=" + getGameName();
    }
}
