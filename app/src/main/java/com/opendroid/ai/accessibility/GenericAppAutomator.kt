package com.opendroid.ai.accessibility

import android.accessibilityservice.AccessibilityService

object GenericAppAutomator {

    suspend fun clickText(text: String): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.findAndClickVerified(text)
    }

    suspend fun clickId(viewId: String): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.findAndClickByIdVerified(viewId)
    }

    suspend fun typeText(searchText: String, content: String): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.findAndTypeVerified(searchText, content)
    }

    fun typeId(viewId: String, content: String): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.findAndTypeById(viewId, content)
    }

    fun scrapeScreen(): String {
        val service = OpenDroidAccessibilityService.getInstance() ?: return ""
        return service.getScreenText()
    }

    fun pressBack(): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun pressHome(): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    fun scroll(forward: Boolean): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.performScroll(forward)
    }

    suspend fun clickCoordinates(x: Float, y: Float): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.clickCoordinatesVerified(x, y)
    }

    fun longPressCoordinates(x: Float, y: Float): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.longPressCoordinates(x, y)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.swipe(x1, y1, x2, y2, durationMs)
    }

    fun swipeDirection(direction: String, durationMs: Long = 300): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.swipeDirection(direction, durationMs)
    }

    suspend fun clickFirstResult(): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        return service.clickFirstResultVerified()
    }
}
