package com.example.myapplication

import java.io.BufferedInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
        if (acceptJob?.isActive == true) {
            return
        }

        acceptJob = scope.launch {
            var retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS
            try {
                while (isActive) {
                    try {
                        retryDelayMillis = runAcceptLoop()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: SocketException) {
                        if (!isActive) {
                            break
                        }
                        retryDelayMillis = handleAcceptFailure(retryDelayMillis)
                    } catch (error: IOException) {
                        if (!isActive) {
                            break
                        }
                        retryDelayMillis = handleAcceptFailure(retryDelayMillis)
                    }
                }
            } finally {
                serverSocket?.closeQuietly()
                serverSocket = null
                acceptJob = null
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

    private fun runAcceptLoop(): Long {
        ServerSocket(port).also { socket ->
            serverSocket = socket
            dataHub.updateConnectionState(ConnectionState.LISTENING)
            while (!socket.isClosed) {
                val client = socket.accept().apply {
                    soTimeout = CLIENT_SOCKET_TIMEOUT_MILLIS
                }
                clientSockets += client
                updateConnectionForActiveClients()
                scope.launch {
                    try {
                        client.use { connectedClient ->
                            readClient(connectedClient)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: SocketTimeoutException) {
                    } catch (_: IOException) {
                    } finally {
                        clientSockets -= client
                        client.closeQuietly()
                        updateConnectionForActiveClients()
                    }
                }
            }
        }
        return INITIAL_RETRY_DELAY_MILLIS
    }

    private suspend fun handleAcceptFailure(retryDelayMillis: Long): Long {
        serverSocket?.closeQuietly()
        serverSocket = null
        dataHub.clearConnectionRuntimeInfo()
        dataHub.updateConnectionState(ConnectionState.ERROR)
        delay(retryDelayMillis)
        return (retryDelayMillis * 2).coerceAtMost(MAX_RETRY_DELAY_MILLIS)
    }

    private fun updateConnectionForActiveClients() {
        val remainingClient = clientSockets.firstOrNull()
        if (remainingClient == null) {
            dataHub.clearConnectionRuntimeInfo()
            serverSocket?.takeIf { !it.isClosed }?.let {
                dataHub.updateConnectionState(ConnectionState.LISTENING)
            }
            return
        }

        dataHub.updateRemoteDeviceIp(remainingClient.inetAddress?.hostAddress)
        dataHub.updateConnectionState(ConnectionState.CONNECTED)
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
        private const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
        private const val MAX_RETRY_DELAY_MILLIS = 10_000L
    }
}
