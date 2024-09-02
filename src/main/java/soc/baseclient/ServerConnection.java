package soc.baseclient;

import soc.client.MainDisplay;
import soc.client.SOCPlayerClient;
import soc.message.SOCVersion;
import soc.util.SOCFeatureSet;
import soc.util.Version;

import java.util.Locale;

public abstract class ServerConnection
{
    /**
     * The client we're communicating for.
     */
    protected SOCBaseClient client;

    /**
     * Server socketConnection info. {@code null} until {@link #connect(String, int)} is called.
     * Unlike {@code connect} params, localhost is not represented here as {@code null}:
     * see {@link ServerConnectInfo#hostname} javadoc.
     *<P>
     * Versions before 2.2.00 instead had {@code host} and {@code port} fields.
     * @since 2.2.00
     */
    protected ServerConnectInfo serverConnectInfo;

    /**
     * For debug, our last message sent over the net.
     *<P>
     * Before v1.1.00 this field was {@code lastMessage}.
     */
    protected String lastMessage;

    /**
     * Any communication exception received while connecting
     * or sending messages in {@link #send(String)}, or null.
     * If {@code lastException != null}, putNet will refuse to send.
     *<P>
     * The exception's {@link Throwable#toString() toString()} including its
     * {@link Throwable#getMessage() getMessage()} may be displayed to the user
     * by {@link SOCPlayerClient#shutdownFromNetwork()}; if throwing an error that the user
     * should see, be sure to set the detail message.
     * @see #ex_P
     */
    protected Exception lastException = null;

    public Exception getLastException()
    {
        return lastException;
    }
    
    public void setLastException( Exception ex )
    {
        lastException = ex;
    }

    /**
     * Send a message to my associated server.
     * @param message a string rendition of the message to the server
     */
    public abstract boolean send( String message );

    /**
     * Create our client's ClientNetwork.
     * Before using the ClientNetwork, caller client must construct their GUI
     * and call {@link #setMainDisplay(MainDisplay)}.
     * Then, call {@link #connect(String, int)}.
     */
    protected ServerConnection( SOCBaseClient c )
    {
        client = c;
        if (client == null)
            throw new IllegalArgumentException( "client is null" );
    }

    /**
     * Construct and send a {@link SOCVersion} message during initial socketConnection to a server.
     * Version message includes features and locale in 2.0.00 and later clients; v1.x.xx servers will ignore them.
     *<P>
     * If debug property {@link SOCPlayerClient#PROP_JSETTLERS_DEBUG_CLIENT_FEATURES}
     * is set, its value is sent instead of {@link #cliFeats}.{@link SOCFeatureSet#getEncodedList()}.
     * Then if debug property {@link SOCDisplaylessPlayerClient#PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P}
     * is set, its value is appended to client features as {@code "com.example.js.feat."} + gameopt3p.
     *
     * The sub class' {@link #send()} method will know whether we are sending this message to a
     * network server or an in-process server.
     *
     * @since 2.8.00
     */
    protected void sendVersion()
    {
        String feats = System.getProperty(SOCPlayerClient.PROP_JSETTLERS_DEBUG_CLIENT_FEATURES);
        if (feats == null)
            feats = client.getClientFeatures().getEncodedList();
        else if (feats.length() == 0)
            feats = null;

        String gameopt3p = System.getProperty(SOCDisplaylessPlayerClient.PROP_JSETTLERS_DEBUG_CLIENT_GAMEOPT3P);
        if (gameopt3p != null)
        {
            gameopt3p = "com.example.js.feat." + gameopt3p.toUpperCase( Locale.US) + ';';
            if (feats != null)
                feats = feats + gameopt3p;
            else
                feats = ';' + gameopt3p;
        }

        final String msg = SOCVersion.toCmd( Version.versionNumber(), Version.version(),
                Version.buildnum(), feats, client.cliLocale.toString());

        send( msg );
    }
}
