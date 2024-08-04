package soc.baseclient;

import soc.client.*;
import soc.game.SOCGame;
import soc.game.SOCGameOptionSet;
import soc.game.SOCPlayer;
import soc.message.SOCMessage;
import soc.util.SOCFeatureSet;
import soc.util.Version;

import java.util.Vector;

abstract public class SOCBaseClient
{
    /**
     *  Server version number for remote server, sent soon after connect, 0 if no server, or -1 if version unknown.
     *  Use {@link #soc.game.getServerVersion(SOCGame)} instead to check the effective version of a specific game.
     *  A local practice server's version is always {@link Version#versionNumber()}, not {@code sVersion},
     *  so always check {@link SOCGame#isPractice} before checking this field.
     * @since 1.1.00
     */
    public int sVersion;

    /**
     * Server's active optional features, sent soon after connect, or null if unknown.
     * Not used with a local practice server, so always check {@link SOCGame#isPractice} before checking this field.
     * @see #tcpServGameOpts
     * @since 1.1.19
     */
    public SOCFeatureSet sFeatures;

    /**
     * Client nickname as a player; null until validated and set by
     * {@link SwingMainDisplay#getValidNickname(boolean)].
     * Returned by {@link #getNickname()}.
     */
    public String nickname = null;

    /**
     * @return the nickname of this user
     */
    public String getNickname()
    {
        return nickname;
    }

    public void setNickname( String n )
    {
        nickname = n;
    }

    /**
     * the password for {@link #nickname} from {@link #pass}, or {@code null} if no valid password yet.
     * May be empty (""). If server has authenticated this password, the {@link #gotPassword} flag is set.
     */
    protected String password = null;

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * True if we've successfully authenticated: Stored the {@link #password} if any, and the server
     * replied that our auth request or join game request with nickname/password was correct.
     * When true, no need to send the password again in later join game/join channel requests.
     * @see #isNGOFWaitingForAuthStatus
     */
    protected boolean authenticated;

    public boolean isAuthenticated()
    {
        return authenticated;
    }

    public void setAuthenticated( boolean bool )
    {
        authenticated = bool;
    }

    /**
     * Face icon ID chosen most recently (for use in new games) for {@link SOCPlayer#setFaceId(int)};
     * always >= {@link SOCPlayer#FIRST_HUMAN_FACE_ID}. Persisted to client pref {@link #PREF_FACE_ICON}
     * by {@link SOCPlayerInterface} window listener's {@code windowClosed(..)}. Even if this client does
     * not show the face it should persist the id for other clients.
     * @since 1.1.00
     */
    protected int lastFaceChange;

    public int getLastFaceChange()
    {
        return lastFaceChange;
    }

    public void setLastFaceChange( int faceId )
    {
        lastFaceChange = faceId;
    }

    /*
        Every client has these two helper classes. MessageHandler accepts incoming messages
        from the server, and handles them appropriately, which may or may not involve a GUI.
        GameMessageSender collects messages specifically for a game and forwards them to a server.
     */
    /**
     * Helper object to dispatch incoming messages from the server.
     * Called by {@link ClientNetwork} when it receives network traffic.
     * Must call {@link MessageHandler#init(SOCPlayerClient)} before usage.
     * @see #gameMessageSender
     */
    protected MessageHandler messageHandler;

    /**
     * Get this client's MessageHandler.
     * @since 2.0.00
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Helper object to form and send outgoing network traffic to the server. For use
     * by the client, no one else.
     * @see #messageHandler
     * @see #net
     * @since 2.0.00
     */
    protected GameMessageSender gameMessageSender;

    /**
     * Get this client's GameMessageSender for making and sending messages to the server.
     * @since 2.0.00
     */
    public GameMessageSender getGameMessageSender()
    {
        return gameMessageSender;
    }

    /**
     * the ignore list
     */
    protected Vector<String> ignoreList = new Vector<String>();

    public Vector<String> getIgnoreList()
    {
        return ignoreList;
    }

    /**
     * @return true if name is on the ignore list
     */
    public boolean onIgnoreList(String name)
    {
        boolean result = false;

        for (String s : ignoreList)
        {
            if (s.equals(name))
            {
                result = true;

                break;
            }
        }

        return result;
    }

    /**
     * add this name to the ignore list
     *
     * @param name the name to add
     * @see #removeFromIgnoreList(String)
     */
    public void addToIgnoreList(String name)
    {
        name = name.trim();

        if (! onIgnoreList(name))
        {
            ignoreList.addElement(name);
        }
    }

    /**
     * remove this name from the ignore list
     *
     * @param name  the name to remove
     * @see #addToIgnoreList(String)
     */
    public void removeFromIgnoreList(String name)
    {
        name = name.trim();
        ignoreList.removeElement(name);
    }

    public void leaveGame( SOCGame game)
    {
        gameMessageSender.leaveGame( game );
    }

    public void resetBoardRequest(SOCGame game)
    {
        gameMessageSender.resetBoardRequest( game );
    }

    // ABSTRACT METHODS

    // put a string to the network connection
//    protected abstract boolean put( String s )
//            throws IllegalArgumentException;
//
//    // send a game message via the game message sender
//    // TODO: refactor out "isPractice"
//    public abstract void putMessage( String toCmd, boolean isPractice );
//
//    public abstract void putMessage( SOCMessage message, boolean isPractice );

    protected abstract int getNumPracticeGames();

    protected abstract PlayerClientListener getClientListener( String gameName );

    protected abstract void addClientListener( String gameName, PlayerClientListener listener );

    protected abstract SOCGameOptionSet getKnownOpts( boolean isPracticeServer );

    protected abstract boolean doesGameExist(final String gameName, final boolean checkPractice);

    protected abstract SOCGameOptionSet getGameOptions( String gameName );

    public abstract int getServerVersion(SOCGame game);
}
