package com.example.myapplication

import java.io.BufferedInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VisionTcpServer(
    private val scope: CoroutineScope,
    private val decoder: VisionStreamDecoder,
    private val dataHub: VisionDataHub = VisionDataHub,
    private val port: Int = DEFAULT_PORT,
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val clientSockets = ConcurrentHashMap.newKeySet<Socket>()

    fun start() {
        if (acceptJob != null) {
            return
        }

        acceptJob = scope.launch {
            runCatching {
                ServerSocket(port).also { socket ->
                    serverSocket = socket
                    dataHub.updateConnectionState(ConnectionState.LISTENING)
                    while (!socket.isClosed) {
                        val client = socket.accept().apply {
                            soTimeout = CLIENT_SOCKET_TIMEOUT_MILLIS
                        }
                        clientSockets += client
                        dataHub.updateRemoteDeviceIp(client.inetAddress?.hostAddress)
                        dataHub.updateConnectionState(ConnectionState.CONNECTED)
                        launch {
                            client.use { connectedClient ->
                                readClient(connectedClient)
                            }
                            clientSockets -= client
                            if (!socket.isClosed) {
                                val remainingClient = clientSockets.firstOrNull()
                                if (remainingClient == null) {
                                    dataHub.clearConnectionRuntimeInfo()
                                    dataHub.updateConnectionState(ConnectionState.LISTENING)
                                } else {
                                    dataHub.updateRemoteDeviceIp(remainingClient.inetAddress?.hostAddress)
                                    dataHub.updateConnectionState(ConnectionState.CONNECTED)
                                }
                            }
                        }
                    }
                }
            }.onFailure {
                if (it !is SocketException) {
                    dataHub.updateConnectionState(ConnectionState.ERROR)
                }
            }
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        clientSockets.forEach { socket -> socket.closeQuietly() }
        clientSockets.clear()
        serverSocket?.closeQuietly()
        serverSocket = null
        dataHub.clearConnectionRuntimeInfo()
        dataHub.updateConnectionState(ConnectionState.STOPPED)
    }

    private fun readClient(client: Socket) {
        BufferedInputStream(client.getInputStream()).use { input ->
            decoder.decode(
                input = input,
                onSensorPacket = dataHub::publishSensorPacket,
                onImageFrame = dataHub::publishImageFrame,
            )
        }
    }

    private fun ServerSocket.closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
        }
    }

    private fun Socket.closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
        }
    }

    companion object {
        const val DEFAULT_PORT = 8080
        private const val CLIENT_SOCKET_TIMEOUT_MILLIS = 10_000
    }
}
