package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Video Player", appName)
  }

  @Test
  fun `format duration helper returns expected string`() {
    assertEquals("00:00", VideoItem.formatDurationMs(0L))
    assertEquals("01:05", VideoItem.formatDurationMs(65000L))
    assertEquals("1:01:05", VideoItem.formatDurationMs(3665000L))
  }
}

