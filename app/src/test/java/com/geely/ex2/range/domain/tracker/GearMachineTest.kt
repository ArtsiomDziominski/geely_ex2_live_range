package com.geely.ex2.range.domain.tracker

import com.geely.ex2.range.domain.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GearMachineTest {
    @Test
    fun falseParkUnderTwoSecondsDoesNotConfirm() {
        val machine = GearMachine()
        machine.tick(0, Gear.DRIVE)
        machine.tick(500, Gear.DRIVE)
        machine.tick(600, Gear.PARK)
        machine.tick(1100, Gear.PARK)
        val events = machine.tick(2500, Gear.DRIVE)
        assertFalse(machine.parkConfirmed)
        assertTrue(events.none { it is GearEvent.LeftPark })
        assertTrue(events.none { it is GearEvent.ConfirmedPark })
    }

    @Test
    fun reverseAfterParkIsLeftParkNotSecondTripStart() {
        val machine = GearMachine()
        machine.tick(0, Gear.PARK)
        machine.tick(500, Gear.PARK)
        machine.tick(2600, Gear.PARK)
        assertTrue(machine.parkConfirmed)
        val leave = machine.tick(3100, Gear.REVERSE)
        val afterDebounce = machine.tick(3600, Gear.REVERSE)
        val drive = machine.tick(4200, Gear.DRIVE)
        val driveStable = machine.tick(4700, Gear.DRIVE)
        val all = leave + afterDebounce + drive + driveStable
        assertEquals(1, all.count { it is GearEvent.LeftPark })
        assertEquals(0, all.count { it is GearEvent.StartedDriving })
    }

    @Test
    fun appStartInDriveEmitsStartedDrivingOnce() {
        val machine = GearMachine()
        val first = machine.tick(0, Gear.DRIVE)
        val second = machine.tick(500, Gear.DRIVE)
        assertTrue(first.none { it is GearEvent.StartedDriving })
        assertEquals(listOf(GearEvent.StartedDriving), second)
        val third = machine.tick(1500, Gear.DRIVE)
        assertTrue(third.isEmpty())
    }
}
