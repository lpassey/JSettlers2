package soc.message;

public class SOCDisconnect extends SOCMessage
{
    /**
     * A message indicating that a connection is disconnecting from its peer.
     */
    public SOCDisconnect()
    {
        messageType = DISCONNECT;
//        super( DISCONNECT );
    }

    @Override
    public String toCmd()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return null;
    }
}
