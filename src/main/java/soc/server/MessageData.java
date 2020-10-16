package soc.server;

import soc.message.SOCMessage;
// import soc.server.genericServer.Connection;
 import soc.communication.Connection;

/**
 * Store a message's contents and sender, and Runnable tasks which must run in a new thread.
 * For simplicity and quick access, final fields are used instead of getters.
 *<br />
 * Before v2.0.00 this class was {@code soc.server.genericServer.Server.Command},
 * with fields {@code str} and {@code con}.
 */
public class MessageData
{
    /**
     * Parsed message contents
     */
    public final SOCMessage message;

    /** Client which sent this message */
    public final Connection clientSender;

    /**
     * Or, some code to run on our Treater thread.
     * If not null, Treater ignores {@link #message} field which would be null anyway.
     * @since 1.2.00
     */
    public final Runnable run;

    public MessageData(final SOCMessage message, final Connection clientSender)
    {
        this.message = message;
        this.clientSender = clientSender;
        this.run = null;
    }

    public MessageData(final Runnable run)
    {
        this.run = run;
        this.message = null;
        this.clientSender = null;
    }
}


