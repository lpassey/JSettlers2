/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * This file Copyright (C) 2008,2010-2012,2014 Jeremy D Monin <jeremy@nand.net>
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
 * Template for per-game message types with no parameters.
 * You will have to write parseDataStr, because of its return
 * type and because it's static.
 *<P>
 * Sample implementation:
 *<code><pre>
 *   public static SOCAdminPing parseDataStr(final String s)
 *   {
 *       return new SOCAdminPing(s);
 *   }
 *</pre></code>
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.00
 */
public abstract class SOCMessageTemplate
{
    private static final long serialVersionUID = 2000L;

    /**
     * Create a new message.
     *
     * @param id  Message type ID
     * @param gameName  Name of game this message is for
     */
    protected SOCMessageTemplate(int id, String gameName) {}

    /**
     * Parse the command String into a MessageType message
     *
     * @param s   the String to parse
     * @return    an AdminPing message
    public static SOCAdminPing parseDataStr(final String s)
    {
        return new SOCAdminPing(s);
    }
     */

}
