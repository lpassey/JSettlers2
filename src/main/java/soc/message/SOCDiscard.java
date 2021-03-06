/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2012,2014,2016-2020 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2017-2018 Strategic Conversation (STAC Project) https://www.irit.fr/STAC/
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

import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;

import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * This message gives the resources that a player has chosen to discard;
 * client's response to server's {@link SOCDiscardRequest}.
 *<P>
 * If the resource total isn't correct, server v2.4.50 and newer will
 * resend {@code SOCDiscardRequest} with the required resource count.
 *<P>
 * If this is the right total amount to discard, server will respond to player
 * with {@link SOCPlayerElement} LOSE messages to confirm the details,
 * then report only the discard's resource total to the other players
 * via {@link SOCPlayerElement} and text.
 *<P>
 * If no other players need to discard, server will then send the new {@link SOCGameState}.
 * If waiting for others to discard, server sends the game a {@link SOCGameServerText} that lists
 * who we're still waiting for. Before v2.0.00, in that case server also sent a redundant
 * {@link SOCGameState}({@link soc.game.GameState#WAITING_FOR_DISCARDS WAITING_FOR_DISCARDS}).
 * Client v1.x.xx correctly displays progress of the discards without that SOCGameState.
 *
 * @author Robert S. Thomas
 */
public class SOCDiscard extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The set of resources being discarded
     */
    private SOCResourceSet resources;

    /**
     * Create a Discard message.
     *
     * @param gameName  the name of the game
     * @param cl  the amount of clay being discarded
     * @param or  the amount of ore being discarded
     * @param sh  the amount of sheep being discarded
     * @param wh  the amount of wheat being discarded
     * @param wo  the amount of wood being discarded
     * @param uk  the amount of unknown resources being discarded
     */
    public SOCDiscard(String gameName, int cl, int or, int sh, int wh, int wo, int uk)
    {
        super( DISCARD, gameName );
        resources = new SOCResourceSet(cl, or, sh, wh, wo, uk);
    }

    /**
     * Create a Discard message.
     *
     * @param gameName  the name of the game
     * @param pn the seat number of the player discarding; currently ignored
     * @param rs  the resources being discarded
     */
    public SOCDiscard(String gameName, int pn, SOCResourceSet rs)
    {
        super( DISCARD, gameName );
        resources = rs;
    }

     /**
     * @return the set of resources being discarded
     */
    public SOCResourceSet getResources()
    {
        return resources;
    }

    /**
     * DISCARD sep game sep2 clay sep2 ore sep2 sheep sep2
     * wheat sep2 wood sep2 unknown
     *
     * @return the command string
     */
    public String toCmd()
    {
        return super.toCmd( sep2 + resources.getAmount(SOCResourceConstants.CLAY) + sep2
            + resources.getAmount(SOCResourceConstants.ORE) + sep2
            + resources.getAmount(SOCResourceConstants.SHEEP) + sep2
            + resources.getAmount(SOCResourceConstants.WHEAT) + sep2
            + resources.getAmount(SOCResourceConstants.WOOD) + sep2
            + resources.getAmount(SOCResourceConstants.UNKNOWN ));
    }

    /**
     * Parse the command String into a Discard message
     *
     * @param s   the String to parse
     * @return    a Discard message, or null if the data is garbled
     */
    public static SOCDiscard parseDataStr(String s)
    {
        ArrayList<String> parsed = parseDataStr( s, 7 );
        if (null != parsed)
        {
            return new SOCDiscard(
                parsed.get( 0 ),    // the game name
                Integer.parseInt( parsed.get( 1 ) ),    // the amount of clay being discarded
                Integer.parseInt( parsed.get( 2 )),     // the amount of ore being discarded
                Integer.parseInt( parsed.get( 3 )),     // the amount of sheep being discarded
                Integer.parseInt( parsed.get( 4 )),     // the amount of wheat being discarded
                Integer.parseInt( parsed.get( 5 )),     // the amount of wood being discarded
                Integer.parseInt( parsed.get( 6 ))      // the amount of unknown resources being discarded
            );
        }
        return null;
/*
        String ga; // the game name
        int cl;
        int or;
        int sh;
        int wh;
        int wo;
        int uk;

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            cl = Integer.parseInt(st.nextToken());
            or = Integer.parseInt(st.nextToken());
            sh = Integer.parseInt(st.nextToken());
            wh = Integer.parseInt(st.nextToken());
            wo = Integer.parseInt(st.nextToken());
            uk = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCDiscard(ga, cl, or, sh, wh, wo, uk);
 */
    }

    /**
     * Strip out the parameter/attribute names from {@link #toString()}'s format,
     * returning message parameters as a comma-delimited list for {@link SOCMessage#parseMsgStr(String)}.
     * @param message Params part of a message string formatted by {@link #toString()}; not {@code null}
     * @return Message parameters without attribute names, or {@code null} if params are malformed
     * @since 2.4.50
     */
    public static String stripAttribNames(String message)
    {
        message = message.replace("resources=",  "");

        return SOCMessage.stripAttribNames(message);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCDiscard:game=" + getGameName() + "|resources=" + resources;
    }

}
