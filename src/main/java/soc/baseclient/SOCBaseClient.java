package soc.baseclient;

import soc.client.*;
import soc.game.SOCGame;
import soc.game.SOCGameOptionSet;
import soc.util.I18n;
import soc.util.SOCFeatureSet;
import soc.util.Version;

import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

abstract public class SOCBaseClient
{
    /**
     * Locale for i18n message lookups used for {@link #strings}.  Also sent to server while connecting.
     * Override if needed in the constructor by reading JVM property. Should only be set by sub-classes
     * {@link I18n#PROP_JSETTLERS_LOCALE PROP_JSETTLERS_LOCALE} ({@code "jsettlers.locale"}).
     * @since 2.0.00
     */
    protected Locale cliLocale;

    public Locale getClientLocale()
    {
        return cliLocale;
    }

    /**
     *  Server version number for remote server, sent soon after connect, 0 if no server, or -1 if version unknown.
     *  Use {@link SOCGame#serverVersion} instead to check the effective version of a specific game.
     *  A local practice server's version is always {@link Version#versionNumber()}, not {@code sVersion},
     *  so always check {@link SOCGame#isPractice} before checking this field.
     * @since 1.1.00
     */
    protected int sVersion;

    public int setServerVersion( int version )
    {
        return sVersion = version;
    }

    /**
     * Server's active optional features, sent soon after connect, or null if unknown.
     * Not used with a local in process server, so always check {@link SOCGame#isPractice} before checking this field.
     * TODO: any reason not to use this with in process servers? Looks like it's never used after setting;
     * should we remove it?
     * @see #tcpServGameOpts
     * @since 1.1.19
     */
    public SOCFeatureSet sFeatures;

    void setServerFeatures( SOCFeatureSet features )
    {
        sFeatures = features;
    }

    /**
     * Features supported by this built-in JSettlers client.
     * @since 2.0.00
     * TODO: hard-coded here. Should we make this dynamic??
     */
    private final SOCFeatureSet cliFeats;
    {
        cliFeats = new SOCFeatureSet(false, false);
        cliFeats.add(SOCFeatureSet.CLIENT_6_PLAYERS);
        cliFeats.add(SOCFeatureSet.CLIENT_SEA_BOARD);
        cliFeats.add(SOCFeatureSet.CLIENT_SCENARIO_VERSION, Version.versionNumber());
    }

    public SOCFeatureSet getClientFeatures()
    {
        return cliFeats;
    }

    /**
     * Client nickname as a player; null until validated and set by
     * {@link SwingMainDisplay#getValidNickname(boolean)].
     * Returned by {@link #getNickname()}.
     */
    protected String nickname = null;

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
        Every client has each these two helper classes. PlayerMessageHandler accepts incoming messages
        from the server, and handles them appropriately, which may or may not involve a GUI.
        GameMessageSender collects messages specifically for a game and forwards them to a server.
     */
    /**
     * Helper object to dispatch incoming messages from the server.
     * Called by {@link ClientNetwork} when it receives network traffic.
     * Must call {@link PlayerMessageHandler#init(SOCPlayerClient)} before usage.
     * @see #gameMessageSender
     */
    protected MessageHandler messageHandler;

    /**
     * Get this client's PlayerMessageHandler.
     * @since 2.0.00
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    // No setter as this is set in the constructor

//    private ServerConnection serverConnection;

    /**
     * Get this client's ServerConnection.
     * @since 2.0.00
     */
//    protected ServerConnection getserverConnection()
//    {
//        return serverConnection;
//    }

    /**
     * Helper object to form and send outgoing network traffic to the server. For use
     * by the client, no one else.
     * @see #messageHandler
     * @see #net
     * @since 2.0.00
     */
    protected GameMessageSender gameMessageSender;

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

    /**
     * All the games we're currently playing. Includes networked or hosted games and those on practice server.
     * Accessed from GUI thread and network {@link PlayerMessageHandler} thread,
     * which sometimes directly calls {@code client.games.get(..)}.
     * @see #serverGames
     */
    protected final Hashtable<String, SOCGame> games = new Hashtable<String, SOCGame>();

    public SOCGame getGame( String gameName )
    {
        return games.get( gameName );
    }

    public SOCGame addGame( SOCGame game )
    {
        SOCGame gamee = games.put( game.getName(), game );
        return gamee;
    }

    public SOCGame removeGame( String gameName )
    {
        return games.remove( gameName );
    }

    public boolean hasGames()
    {
        return ! games.isEmpty();
    }

    /**
     * Get this client's GameMessageSender for making and sending messages to the server.
     * @since 2.0.00
     */
    public GameMessageSender getGameMessageSender()
    {
        return gameMessageSender;
    }

    public void leaveGame( SOCGame game)
    {
        gameMessageSender.leaveGame( game );
    }

    public void resetBoardRequest(SOCGame game)
    {
        gameMessageSender.resetBoardRequest( game );
    }

    /**
     * True if contents of incoming and outgoing network message traffic should be debug-printed.
     * Set if optional system property {@link SOCDisplaylessPlayerClient#PROP_JSETTLERS_DEBUG_TRAFFIC} is set.
     *<P>
     * Versions earlier than 1.1.20 always printed this debug output; 1.1.20 never prints it.
     * @since 1.2.00
     */
    public boolean debugTraffic;

    public boolean anyHostedActiveGames()
    {
        return false;
    }

    // ABSTRACT METHODS
//    protected abstract int getNumPracticeGames();

//    public abstract PlayerClientListener getClientListener( String gameName );

//    protected abstract void addClientListener( String gameName, PlayerClientListener listener );

    protected abstract SOCGameOptionSet getKnownOpts( boolean isPracticeServer );

    protected abstract boolean doesGameExist(final String gameName, final boolean checkPractice);

    protected abstract SOCGameOptionSet getGameOptions( String gameName );

    public abstract int getServerVersion(SOCGame game);

    /**
     * network trouble; if possible, ask if they want to play locally (practiceServer vs. robots).
     * Otherwise, go ahead and shut down. Either way, calls {@link MainDisplay#showErrorPanel(String, boolean)}
     * to show an error message or network exception detail.
     * Removes server's games and channels from MainDisplay's lists.
     *<P>
     * "If possible" is determined from return value of {@link ClientNetwork#putLeaveAll()}.
     *<P>
     * Before v1.2.01 this method was {@code destroy()}.
     */
    public abstract void shutdownFromNetwork();

    public abstract void disconnect();

    public abstract void requestAuthorization();
}
