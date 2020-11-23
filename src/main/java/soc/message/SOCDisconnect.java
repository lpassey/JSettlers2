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

    /**
     * <ADMINRESET>
     *
     * @return the command String
     */
    @Override
    public String toCmd()
    {
        return Integer.toString( DISCONNECT );
    }

    /**
     * Parse the command String into a AdminReset message
     *
     * @param s   the String to parse; contents ignored, since this message has no parameters
     * @return    a AdminReset message
     */
    public static SOCDisconnect parseDataStr( String s )
    {
        return new SOCDisconnect();
    }


    @Override
    public String toString()
    {
        return "SOCDisconnect:";
    }
}
