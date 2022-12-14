package server

import game.Player
import game.Status
import game.fmt
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import java.net.SocketAddress

/**
 *  Class representing a connection to a player.
 */
data class Connection(var address: SocketAddress, var player: Player, var timeOfLastPackage: Timer = Timer()) {
    var lastPacket: Packet<ReceiveCommand>? = null

    /**
     * Performs actions corresponding to the most recent [Packet] received from the player.
     */
    fun handleInput() {
        // Returns if packet is invalid.
        val (cmd, data) = lastPacket ?: return

        try {
            when (cmd) {
                ReceiveCommand.MOVE -> {
                    val direction = fmt.decodeFromString<Direction>(data)
                    player.move(direction)
                }
                ReceiveCommand.INTERACT_WITH_FOOD_BOX -> {
                    player.interactWithFoodBox()
                }
                ReceiveCommand.DISCONNECTED -> {
                    sendBothPlayers(SendCommand.GAME_ABORTED)
                    Server.gameState.status = Status.ABORTED
                }
                else -> {}
            }
        } catch (e: SerializationException) {
            /* no-op */
        }
        // Sets lastPacket to null, since it has been handled
        lastPacket = null
    }
}

/**
 *  Contains a nullable Connection for each player
 */
data class Connections(var player1: Connection? = null, var player2: Connection? = null) {

    /**
     *  Returns a connection that hasn't sent a valid packet for a duration longer than [SECONDS_UNTIL_TIMED_OUT].
     *  If none exist, returns null.
     */
    fun getTimedOut(): Connection? {
        val p1t = player1?.timeOfLastPackage?.elapsedSeconds
        val p2t = player2?.timeOfLastPackage?.elapsedSeconds

        if (p1t != null) {
            if (p1t > SECONDS_UNTIL_TIMED_OUT) return player1
        }

        if (p2t != null) {
            if (p2t > SECONDS_UNTIL_TIMED_OUT) return player2
        }

        return null
    }


    /**
     * Handles the most recent [Package] received from each player.
     */
    fun handleInput() {
        player1?.handleInput()
        player2?.handleInput()
    }
}


/**
 *  Establishes a new connection with a client, returning their [Player] name and [SocketAddress]
 */
fun newConnection(): Pair<String, SocketAddress> {
    while (true) {
        val datagram = read()

        // If data recieved does not conform to the protocol, try again
        val (pckt, addr) = datagram.extractWithAddress() ?: continue
        if (pckt.command == ReceiveCommand.CONNECTION_REQUEST) {
            val name = pckt.data
            // Makes sure that the same client is not attempting to connect twice.
            if (addr.isCurrentPlayer()) {
                addr.send(SendCommand.CONNECTION_DENIED)
                continue
            } else {
                addr.send(SendCommand.CONNECTION_ACCEPTED)
                return name to addr
            }
        } else {
            continue
        }
    }
}


/**
 *  Establishes new connections to two clients, and returning corresponding [Player] objects
 */
fun connectPlayers(): Pair<Player, Player> {
    val (name1, addr1) = newConnection()
    val (name2, addr2) = newConnection()

    val p1 = Player(1, name1)
    val p2 = Player(2, name2)
    Server.connections.player1 = Connection(addr1, p1)
    Server.connections.player2 = Connection(addr2, p2)
    return p1 to p2
}

/**
 *  Returns a boolean telling whether _this_ [SocketAddress] is currently connected to the game
 */
fun SocketAddress.isCurrentPlayer(): Boolean {
    return (this == Server.connections.player1?.address || this == Server.connections.player2?.address)
}