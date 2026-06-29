package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.viewmodel.LmConnectViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LM Connect", appName)
  }

  @Test
  fun testViewModelInstantiation() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LmConnectViewModel(application)
    assertNotNull(viewModel)
  }
}
