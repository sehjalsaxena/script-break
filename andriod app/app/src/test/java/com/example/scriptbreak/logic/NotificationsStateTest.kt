package com.example.scriptbreak.logic

import com.example.scriptbreak.ml.ScamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The bell on Home: "Clear all" and read marks are kept when you leave the screen, separately for each person. */
class NotificationsStateTest {
    private val model = ScamModel.fromJson(File("src/main/assets/scam_model.json").readText())

    private class Quiet : SignalSource {
        override val callNow = false
        override val screenShare = false
        override fun callSeenWithin30m(nowMs: Long) = false
        override fun isNight(nowMs: Long) = false
    }

    @Test
    fun clearAllStaysClearedAfterLeavingTheScreen() {
        val f = PaymentFlow(model, Quiet(), { }).apply { login("sambhav", "1111") }
        f.startNotifications()
        f.readNotice("2")
        f.clearNotices(listOf("1", "2", "3", "4", "5"))
        f.back()                                                          // leave the screen
        assertEquals(Screen.Home, f.screen)
        f.startNotifications()                                            // and come back
        assertEquals(setOf("1", "2", "3", "4", "5"), f.clearedNoticeIds)
        assertTrue("2" in f.readNoticeIds)

        f.logout(); f.login("deepak", "2222")                             // each person has their own
        assertTrue(f.clearedNoticeIds.isEmpty())
        f.logout(); f.login("sambhav", "1111")
        assertEquals(5, f.clearedNoticeIds.size)
        f.resetDemo()
        assertTrue("Reset demo brings them back", f.clearedNoticeIds.isEmpty())
    }
}
