/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2014,2017-2020 Jeremy D Monin <jeremy@nand.net>
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


/**
 * This message is a way for the admin to test
 * if a robot is connected and running
 * in a game where the admin user is a member.
 *<P>
 * Can be used as a rough bot-development/debug tool:
 * If a robot client hears this ping and is already in the game
 * with the admin/debug player that sent it, respond with "OK";
 * Otherwise join that game.
 *
 * @author Robert S Thomas
 * @see SOCServerPing
 * @see SOCTimingPing
 */
public class SOCAdminPing extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * Create a AdminPing message.
     *
     * @param ga  name of game
     */
    public SOCAdminPing(String ga)
    {
        super( ADMINPING, ga );
    }

    /**
     * ADMINPING sep game -- super method is good enough
     *
     * @return the command String
     */
//    public String toCmd()
//    {
//        return ADMINPING + sep + game;
//    }

    /**
     * Parse the command String into a AdminPing message
     *
     * @param s   the String to parse
     * @return    an AdminPing message
     */
    public static SOCAdminPing parseDataStr(String s)
    {
        return new SOCAdminPing(s);
    }

    /**
     * @return a human readable form of the message
     */
    @Override
    public String toString()
    {
        return "SOCAdminPing:game=" + getGame();
    }

}
