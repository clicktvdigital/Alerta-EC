package org.breezyweather.ui.map

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InamhiWmsDownloadTest {
    @Test
    fun cancellationCancelsUnderlyingCallEvenWithoutNetworkCallback() = runBlocking {
        val call = mockk<Call>(relaxed = true)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { InamhiWmsTimes.download(call) }
        job.cancelAndJoin()
        verify(exactly = 1) { call.cancel() }
        verify(exactly = 1) { call.enqueue(any()) }
    }

    @Test
    fun networkErrorPropagates() {
        val call = mockk<Call>(relaxed = true)
        every { call.enqueue(any()) } answers {
            firstArg<Callback>().onFailure(call, IOException("timeout"))
        }
        assertThrows(IOException::class.java) { runBlocking { InamhiWmsTimes.download(call) } }
    }

    @Test
    fun httpFailureIsNotParsedAsEmptyCatalog() {
        val call = mockk<Call>(relaxed = true)
        every { call.enqueue(any()) } answers {
            firstArg<Callback>().onResponse(call, Response.Builder()
                .request(Request.Builder().url("https://example.invalid/").build())
                .protocol(Protocol.HTTP_1_1).code(503).message("Unavailable")
                .body("<error/>".toResponseBody()).build())
        }
        assertThrows(IllegalStateException::class.java) { runBlocking { InamhiWmsTimes.download(call) } }
    }
}
