package com.arkade.network.grpc

import com.arkade.core.bitcoin.Coin
import com.arkade.core.bitcoin.Network
import com.arkade.network.ArkadeClient
import com.arkade.network.Config
import com.arkade.utils.Log
import com.arkade.utils.info
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class ArkadeClientTest : com.arkade.Test() {
    @Test
    fun can_fetch_server_info_from_mainnet() =
        runTest {
            val arkadeClient: ArkadeClient = ArkadeClientImpl(Config.MAINNET)
            val mainnetServerPubKey = "022b74c2011af089c849383ee527c72325de52df6a788428b68d49e9174053aaba"
            val mainnetForfeitAddress = "bc1qzzdzp5c443vsetzatf2ra6hku322y7e5aq50rs"

            val serverInfo = arkadeClient.getInfo()

            Log.info("Server info", "${serverInfo.fees?.intent}")

            assertEquals(Network.MAINNET, serverInfo.network)
            assertEquals(
                mainnetServerPubKey,
                serverInfo.signerPubKey
                    .publicKey
                    .toHex(),
            )
            assertEquals(mainnetForfeitAddress, serverInfo.forfeitAddress.encode())
            assertEquals(Coin.fromSatoshi(330), Coin.fromSatoshi(serverInfo.dust))
            assertEquals(90.days.inWholeDays, serverInfo.boardingExitDelay.inWholeDays)
            assertEquals(7.days.inWholeDays, serverInfo.unilateralExitDelay.inWholeDays)
            assertEquals(1.minutes.inWholeMinutes, serverInfo.sessionDuration.inWholeMinutes)
        }
}
