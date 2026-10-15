package com.applycoding.pos.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applycoding.pos.Cart
import com.applycoding.pos.PosApi
import com.applycoding.pos.android.data.OrderRepository
import com.applycoding.pos.android.data.PosDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Room (in memory) + offline first sync against a mocked GraphQL server. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OrderRepositoryTest {
    private lateinit var db: PosDatabase
    private var online = true
    private var calls = 0

    private val api = PosApi(
        HttpClient(MockEngine {
            calls++
            if (!online) respondError(HttpStatusCode.ServiceUnavailable)
            else respond("""{"data":{"submitOrder":{"id":"srv_$calls","totalCents":0}}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }),
        "http://pos/graphql",
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PosDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    @Test fun checkoutStoresOrderWithTotals() = runTest {
        val repo = OrderRepository(db.orders(), null)
        val order = repo.checkout(Cart().add(SampleCatalog.products[5]))
        assertEquals(1476L, order.grossCents) // 12.00 + 23%
        assertEquals(1476L, repo.takings())
        assertEquals(1, repo.orders.first().size)
    }

    @Test fun offlineOrdersSyncWhenBackOnline() = runTest {
        val repo = OrderRepository(db.orders(), api)
        online = false
        repo.checkout(Cart().add(SampleCatalog.products[0]))
        repo.checkout(Cart().add(SampleCatalog.products[1]))
        assertEquals(0, repo.sync())
        assertEquals(2, db.orders().pending().size)
        online = true
        assertEquals(2, repo.sync())
        assertEquals(0, db.orders().pending().size)
        assertNotNull(repo.orders.first().first().serverId)
    }
}
