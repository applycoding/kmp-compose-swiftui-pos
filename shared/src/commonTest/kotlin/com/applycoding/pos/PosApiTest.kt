package com.applycoding.pos

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PosApiTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun parsesProducts() = runTest {
        val engine = MockEngine { req ->
            assertTrue(req.body.toByteArray().decodeToString().contains("products"))
            respond("""{"data":{"products":[{"sku":"COF","name":"Flat white","priceCents":350,"vat":"SECOND_REDUCED","category":"Drinks"}]}}""", headers = jsonHeaders)
        }
        val products = PosApi(HttpClient(engine), "http://pos/graphql").products()
        assertEquals("Flat white", products.single().name)
        assertEquals(VatRate.SECOND_REDUCED, products.single().vatRate)
    }

    @Test fun sendsVariablesForSubmit() = runTest {
        var sent = ""
        val engine = MockEngine { req ->
            sent = req.body.toByteArray().decodeToString()
            respond("""{"data":{"submitOrder":{"id":"ord_1","totalCents":431}}}""", headers = jsonHeaders)
        }
        val order = PosApi(HttpClient(engine), "http://pos/graphql").submitOrder("c-1", listOf(OrderLineInput("COF", 1)))
        assertEquals("ord_1", order.id)
        assertTrue(sent.contains("\"clientOrderId\":\"c-1\""))
        assertTrue(sent.contains("\"sku\":\"COF\""))
    }

    @Test fun graphQlErrorsBecomeExceptions() = runTest {
        val engine = MockEngine { respond("""{"errors":[{"message":"out of stock"}]}""", headers = jsonHeaders) }
        val e = assertFailsWith<GraphQlException> { PosApi(HttpClient(engine), "http://pos/graphql").products() }
        assertEquals("out of stock", e.message)
    }
}
