/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2013-2014,2016-2021 Jeremy D Monin <jeremy@nand.net>
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

import java.util.ArrayList;


/**
 * This message means that the player is accepting an offer.
 *<P>
 * Sent from accepting player's client to server.
 * If the trade is allowed, announced from server to all players.
 *<UL>
 * <LI> Client message to server is in response to a {@link SOCMakeOffer} announced earlier this turn
 *      with client as an offered-to player.
 * <LI> Server's response (announced to game) is {@link SOCPlayerElement}s, {@code SOCAcceptOffer},
 *      {@link SOCGameTextMsg} to v1.x clients, then {@link SOCClearOffer}s.
 *</UL>
 *<P>
 * The server disallows any unacceptable trade by sending that "accepting" client a
 * {@code SOCRejectOffer} with reason code {@link SOCRejectOffer#REASON_CANNOT_MAKE_TRADE}.
 * Servers before v2.4.50 ({@link SOCRejectOffer#VERSION_FOR_REPLY_REASONS})
 * sent an explanatory {@link SOCGameServerText} instead.
 *<P>
 * Only v1.x clients are sent the {@code SOCGameTextMsg}, which conveys the same info as this {@code SOCAcceptOffer}.
 * Before v2.4.50 the server announced {@code SOCGameTextMsg} before {@code SOCAcceptOffer}, instead of after.
 *<P>
 * Before v2.0.00 the server announced the {@code SOCClearOffer}s before {@code SOCAcceptOffer}. The old
 * non-robot clients ignored that {@code SOCAcceptOffer}, so changing the order has no effect on them.
 *
 * @author Robert S. Thomas
 * @see SOCRejectOffer
 */
public class SOCAcceptOffer extends SOCMessageForGame
{
    private static final long serialVersionUID = 1111L;  // last structural change v1.1.11

    /**
     * The accepting player number from server:
     * see {@link #getAcceptingNumber()}.
     */
    private int accepting;

    /**
     * The offering player number; see {@link #getOfferingNumber()}.
     */
    private int offering;

    /**
     * Create an AcceptOffer message.
     *
     * @param gameName  the name of the game
     * @param ac  the player number of the accepting player;
     *     always ignored if sent from client.
     *     See {@link #getAcceptingNumber()}.
     * @param of  the player number of the offering player
     */
    public SOCAcceptOffer(String gameName, int ac, int of)
    {
        super( ACCEPTOFFER, gameName );
        accepting = ac;
        offering = of;
    }

    /**
     * When sent from server, get the player number accepting the trade offered by {@link #getOfferingNumber()}.
     * From client, server has always ignored this field; could be any value.
     * @return the number of the accepting player from server,
     *     or any value sent from client (server has always ignored this field)
     */
    public int getAcceptingNumber()
    {
        return accepting;
    }

    /**
     * Get the player number offering this trade which is
     * being accepted by {@link #getAcceptingNumber()}.
     * @return the number of the offering player
     */
    public int getOfferingNumber()
    {
        return offering;
    }

    /**
     * ACCEPTOFFER sep game sep2 accepting sep2 offering
     *
     * @return the command string
     */
    @Override
    public String toCmd()
    {
        return super.toCmd( sep2 + accepting + sep2 + offering );
    }

    /**
     * Parse the command String into an ACCEPTOFFER message.
     *
     * @param s   the String to parse
     * @return    an ACCEPTOFFER message, or null if the data is garbled
     */
    public static SOCAcceptOffer parseDataStr(String s)
    {
        ArrayList<String> parsed = parseDataStr( s, 3 );
        if (null != parsed)
        {
            return new SOCAcceptOffer( parsed.get( 0 ), Integer.parseInt( parsed.get( 1 ) ),
                Integer.parseInt( parsed.get( 2 ) ) );
        }
        return null;
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCAcceptOffer:game=" + getGameName() + "|accepting=" + accepting + "|offering=" + offering;
    }

}
