package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.ProductEntity
import com.example.data.model.CartItem
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kasir Retail", appName)
  }

  @Test
  fun `cart calculations and profit margins are correct`() {
    val product = ProductEntity(
      id = 1,
      name = "Minyak Goreng 2L",
      barcode = "8998866100012",
      buyPrice = 30000.0,
      sellPrice = 36000.0,
      stock = 10,
      minStockAlert = 5
    )

    val cartItem = CartItem(product = product, quantity = 3)
    assertEquals(108000.0, cartItem.subtotal, 0.01)
    assertEquals(90000.0, cartItem.totalCost, 0.01)
    assertEquals(18000.0, cartItem.profit, 0.01)
  }

  @Test
  fun `change calculation is correct`() {
    val total = 45000.0
    val cashPaid = 50000.0
    val change = (cashPaid - total).coerceAtLeast(0.0)
    assertEquals(5000.0, change, 0.01)
  }

  @Test
  fun `rupiah formatter works as expected`() {
    val formatted = Formatters.formatRupiah(50000.0)
    assertTrue(formatted.contains("50.000") || formatted.contains("50,000"))
  }
}
