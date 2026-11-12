package com.nangmanroad.phone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarDemoTest {
    @Test
    fun sleepModeRestoresThePreviousSettings() {
        val previous = CarDemoState(doorsLocked = false, climateOn = true, cabinTemp = 23, targetTemp = 24)

        val sleeping = previous.apply(CarCommand.START_SLEEP)

        assertTrue(sleeping.doorsLocked)
        assertFalse(sleeping.interiorLightOn)
        assertFalse(sleeping.exteriorLightOn)
        assertEquals(21, sleeping.targetTemp)
        assertEquals(sleeping, sleeping.apply(CarCommand.START_SLEEP))
        assertEquals(previous, sleeping.apply(CarCommand.END_SLEEP))
    }

    @Test
    fun sleepModeRequiresParkedState() {
        val moving = CarDemoState(parked = false)

        assertEquals(moving, moving.apply(CarCommand.START_SLEEP))
    }
}
