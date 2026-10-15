package com.applycoding.pos

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class GraphQlRequest(val query: String, val variables: Map<String, JsonElement> = emptyMap())

@Serializable
data class GraphQlError(val message: String)

@Serializable
data class GraphQlResponse(val data: JsonObject? = null, val errors: List<GraphQlError>? = null)

class GraphQlException(message: String) : Exception(message)

@Serializable
data class OrderLineInput(val sku: String, val quantity: Int)

@Serializable
data class SubmittedOrder(val id: String, val totalCents: Long)

/**
 * Small typed GraphQL client shared by Android and iOS. The HTTP engine is injected (OkHttp on
 * Android, Darwin on iOS, MockEngine in tests).
 */
class PosApi(engineClient: HttpClient, private val endpoint: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = engineClient.config { install(ContentNegotiation) { json(json) } }

    suspend fun execute(query: String, variables: Map<String, JsonElement> = emptyMap()): JsonObject {
        val res: GraphQlResponse = http.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(GraphQlRequest(query, variables))
        }.body()
        res.errors?.takeIf { it.isNotEmpty() }?.let { throw GraphQlException(it.joinToString { e -> e.message }) }
        return res.data ?: throw GraphQlException("empty response")
    }

    suspend fun products(): List<Product> {
        val data = execute(PRODUCTS)
        return json.decodeFromJsonElement(data["products"] ?: throw GraphQlException("missing products"))
    }

    suspend fun submitOrder(clientOrderId: String, lines: List<OrderLineInput>): SubmittedOrder {
        val vars = mapOf(
            "clientOrderId" to JsonPrimitive(clientOrderId),
            "lines" to json.encodeToJsonElement(lines),
        )
        val data = execute(SUBMIT, vars)
        return json.decodeFromJsonElement(data["submitOrder"]?.jsonObject ?: throw GraphQlException("missing submitOrder"))
    }

    companion object {
        const val PRODUCTS = "query Products { products { sku name priceCents vat category } }"
        const val SUBMIT = "mutation Submit(\$clientOrderId: ID!, \$lines: [OrderLineInput!]!) { submitOrder(clientOrderId: \$clientOrderId, lines: \$lines) { id totalCents } }"
    }
}
