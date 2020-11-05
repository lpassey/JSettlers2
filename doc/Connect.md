#
## JSettlers connection model

JSettlers is, by design a client/server application. The server side is responsible for maintaining game state and enforcing game rules. The client side is responsible solely for displaying the game board and pieces, together with appropriate data about each player, their actions, and capabilities as directed by the game server.

In this iteration, communication between the client and the server is handled by matching pairs of `Connection` objects, one each on the client and server sides. For clients and servers running in the same process space communication is handled by instances of `MemConnection`, and for clients and servers communicating over a network connection communication is handled by instances of `NetConnection`. 

All classes that are used for client/server communication are found in the package `soc.communication`.

### The `Connection` Class

The abstract `Connection` class defines the methods that are used to pass message back and forth from and to clients and servers. Generally, code that sends or retrieves messages need not be aware of the sub-class being used; the `Connection`
interface should be all that is necessary.
 
 The primary `Connection` methods used are `startMessageProcessing(SOCMessageDispatcher)` and `send(SOCMessage)`. 
 
 `SOCMessageDispatcher` is an interface that defines two methods: `dispatch(SOCMessage, Connection)` and `dispatchFirst(SOCMessage, Connection)`. Instances of `SOCMessageDispatcher` differ whether the connection is attached to a client (human or robot) or a server. Calling `Connection.startMessageProcessing()` passing a concrete instance of a `SOCMessageDispatcher` starts a new thread that receives a message from a peer `Connection` and then calls the appropriate method on the `SOCMessageDispatcher` instance.
 
 Current server code expects the first message received to be handled differently than subsequent messages; hence the two separate methods. Typically clients can route both methods to the same handler routines.
 
 Note: `SOCMessageDispatcher` derived instances run in a thread for each `Connection` instance, so _do not block or sleep_ in any of the code called from either of the `dispatch` methods. Any slow or lengthy work for a message should be done on other threads.
  
 The `Connection.send()` method is used to send an `SOCMessage`-derived message to a connection's peer. The peer's `SOCMessageDispatcher` thread will accept the message and dispatch it to the appropriate message handler.
 
 A `Connection` instance cannot be used to send messages until it's peer's `startMessageProcessing(SOCMessageDispatcher)` method has been called.
 
 `Connection.connect()` is used to "wire up" any internal data structures required for network communication. It must be called before messages can be sent over a network connection.
 
 `Connection.disconnect()` is used to end a communication session. After the `disconnect()` method has been called no further communication can occur between the peers of this connection.
 
 There are a number of other methods defined by the Connection class; primarily getters and setters for class properties. Consult the javadocs for the `Connection` class for more details.

## The `MemConnection` class

The `MemConnection` class derives from `Connection` and is used for communication between clients and servers which exist in the same process space. Default robots run in the same process space as the server, so these robots connect to the server using `MemConnection` even when other clients may connect via a TCP network connection.

`MemConnection` is implemented with a Java `LinkedBlockingQueue`, and declares the method `receive(SOCMessage)` to place messages into the queue. `MemConnection.run()` takes messages from the `waitQueue` and dispatches them using the connection's `SOCMessageDispatcher` instance.

When a client's `MemConnection` instance is connected to a server running in the same process space the server creates a new `MemConnection` peer, sets its `ourPeer` instance variable to the client's `MemConnection` instance, and sets the client's `ourPeer` instance variable to the newly created server `MemConnection` instance. Each connection's `accepted` flag is also set to 'true`.

`MemConnection.send(SOCMessage)` is implemented by simply calling `ourPeer.receive(SOCMessage)`.

`MemConnection.connect()` simply stores a timestamp of when the connection occurred, and returns the state of the `accepted` flag.

`MemConnection.disconnect()` resets `ourPeer`'s peer instance (which should be equal to `this`) to null, sends a `SOCDisconnect` message to the peer, and interrupts the dispatcher thread, which will cause it to exit. Upon receiving the `SOCDisconnect` message the peer should call its own `disconnect()` method. Its own peer was previously set to null so it will not reply with another `SOCDisconnect` message.

## The `NetConnection` class

The `NetConnection` class derives from `Connection` and is used for communication between clients and servers using the TCP/IP protocol. It is implmented using `DataInputStream` and `DataOutputStream` instances for communication. It requires a connected `java.net.Socket` instance in its constructor. 

The server creates a socket by setting up a `java.net.ServerSocket` and then calling its `accept()` method, which will not return until a client has  made a network connection.

A client creates a socket by calling `Socket.connect()` with the network details of the server.

Once both sides have instaniated a new `NetConnection` instance each calls `connect()`, which links the internal Data*Streams to the socket's input and output streams. Once `startMessageProcessing(SOCMessageDispatcher)` has been called on both `NetConnection` instances, client/server network communication can begin.

Only strings may be sent over the network between servers and clients. When called, the `MemConnection.send(SOCMessage)` begins by calling `SOCMessage.toCmd()` to generate a string version of the message. This string must be generated in such a way that the message can be recreated by calling `SOCMessage.toMsg()`. The string version is then sent to the peer using the `DataOutputStream.writeUTF(String)` method.

The message dispatcher thread in `NetConnection` reads messages as they arrive using the `DataInputStream.readUTF()` method. Each string message is converted to the appropriate `SOCMessage` derivative using the `SOCMessage.toMsg()` method. The message is then delivered to the `SOCMessageDispatcher` instance as described above.

`NetConnection.disconnect()` sends a `SOCDisconnect` message to its network peer, interrupts its `SOCMessageDispatcher` thread then closes its socket. It cannot be used for client/server communication thereafter.
