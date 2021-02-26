/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2012,2014,2017 Jeremy D Monin <jeremy@nand.net>
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
 * This message from server to a client requests that the player discard a particular number of cards.
 * Client should respond with {@link SOCDiscard}.
 *<P>
 * Same prompt/response pattern as
 * {@link SOCSimpleRequest}({@link SOCSimpleRequest#PROMPT_PICK_RESOURCES PROMPT_PICK_RESOURCES})
 * / {@link SOCPickResources}.
 *
 * @author Robert S. Thomas
 */
public class SOCDiscardRequest extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The number of discards
     */
    private int numDiscards;

    /**
     * Create a DiscardRequest message.
     *
     * @param gameName  the name of the game
     * @param nd  the number of discards
     */
    public SOCDiscardRequest(String gameName, int nd)
    {
        super( DISCARDREQUEST, gameName );
        numDiscards = nd;
    }

    /**
     * @return the number of discards
     */
    public int getNumberOfDiscards()
    {
        return numDiscards;
    }

    /**
     * DISCARDREQUEST sep game sep2 numDiscards
     *
     * @return the command string
     */
    public String toCmd()
    {
        return super.toCmd(sep2 + numDiscards );
    }

    /**
     * Parse the command String into a DiscardRequest message
     *
     * @param s   the String to parse
     * @return    a DiscardRequest message, or null if the data is garbled
     */
    public static SOCDiscardRequest parseDataStr(String s)
    {
        SOCMessageTemplate1i result = SOCMessageTemplate1i.parseDataStr( DISCARDREQUEST, s );

        String ga; // the game name
        int nd; // the number of discards

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            nd = Integer.parseInt(st.nextToken());
//            return new SOCDiscardRequest(ga, nd);
        }
        catch (Exception e)
        {
            return null;
        }

        if (null != result)
            return new SOCDiscardRequest( result.getGameName(), result.p1 );
        return null;
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCDiscardRequest:game=" + getGameName() + "|numDiscards=" + numDiscards;
    }
}
