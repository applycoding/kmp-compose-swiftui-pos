package com.applycoding.pos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.applycoding.pos.PosApi
import com.applycoding.pos.android.data.OrderRepository
import com.applycoding.pos.android.data.PosDatabase
import com.applycoding.pos.android.ui.PosScreen
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = PosDatabase.create(applicationContext)
        val api = PosApi(HttpClient(OkHttp), BuildConfig.GRAPHQL_URL)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PosViewModel(OrderRepository(db.orders(), api), api) as T
        }
        setContent {
            MaterialTheme {
                Surface {
                    val vm: PosViewModel = viewModel(factory = factory)
                    val state = vm.state.collectAsStateWithLifecycle().value
                    PosScreen(state, vm::add, vm::setQuantity, vm::applyDiscount, vm::pay)
                }
            }
        }
    }
}
