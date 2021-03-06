/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2012-2014,2016-2020 Jeremy D Monin <jeremy@nand.net>
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
 * This message means that someone is leaving a game.
 * Sent from leaving client to server (if leaving cleanly),
 * then sent from server out to all clients in game.
 *<P>
 * Although this is a game-specific message, it's about the game lifecycle
 * so it's handled by {@code SOCServer} instead of a {@code GameHandler}.
 *
 * @author Robert S Thomas
 * @see SOCLeaveChannel
 */
public class SOCLeaveGame extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * Nickname of the leaving member; server has always ignored this field from client, can send "-" but not blank
     */
    private String nickname;

    /**
     * Unused optional host name of server hosting the game, or "-"; see {@link #getHost()}.
     */
    private String host;

    /**
     * Create a LeaveGame message.
     *
     * @param gameName  name of game
     * @param nn  leaving member's nickname; server has always ignored this field from client, can send "-" but not blank
     * @param hn  unused optional host name, or "-"
     *            (Length 0 would fail {@link #parseDataStr(String)} at the receiver)
     */
    public SOCLeaveGame( String gameName, String nn, String hn )
    {
        super( LEAVEGAME, gameName );
        nickname = nn;
        host = hn;
    }

    /**
     * @return the nickname; can be "-" but not blank when sent from client
     */
    public String getNickname()
    {
        return nickname;
    }

    /**
     * Get the unused optional host name of server hosting the game, or "-".
     * Is always "-" when sent from server v1.1.17 or newer, or client v2.0.00 or newer.
     * @return the host name, or "-"
     * @see SOCMessageTemplateJoinGame#getHost()
     */
    public String getHost()
    {
        return host;
    }

    /**
     * NOTE: non-standard ordering of fields in message; beware if you change this!
     *
     * LEAVEGAME sep nickname sep2 host sep2 game
     *
     * @return the command String
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + nickname + sep2 + host );
    }

    /**
     * Parse the command String into a LeaveGame message.
     *
     * @param s   the String to parse
     * @return    a LeaveGame message, or null if the data is garbled
     */
    public static SOCLeaveGame parseDataStr(String s)
    {
        String nickname; // nickname
        String host; // host name
        String gameName; // game name

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            gameName = st.nextToken();
            nickname = st.nextToken();
            host = st.nextToken();
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCLeaveGame( gameName, nickname, host );
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCLeaveGame:game=" + getGameName() + "|nickname=" + nickname + "|host=" + host;
    }
}
