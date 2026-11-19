package com.example.deviceowner

import com.example.deviceowner.DeviceOwnerPolicy.Restriction
import com.example.deviceowner.DeviceOwnerPolicy.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Policy tests for the Device Owner protection — item #11: the definitive
 * ADB/computer-uninstall block. Only the pure decisions are testable here;
 * the real DevicePolicyManager calls need a provisioned device.
 */
class DeviceOwnerPolicyTest {

    // ---- Restriction set computation ----

    @Test
    fun restrictions_deviceOwnerAndToggleOn_appliesBothBlocks() {
        val desired = DeviceOwnerPolicy.desiredRestrictions(
            isDeviceOwner = true,
            toggleEnabled = true
        )
        assertEquals(
            setOf(Restriction.BLOCK_OWN_UNINSTALL, Restriction.DISALLOW_DEBUGGING),
            desired
        )
    }

    @Test
    fun restrictions_toggleOff_clearsExactlyTheManagedSet() {
        // OFF means "clear what we applied" — the empty desired set is
        // what makes reconcile() call setUninstallBlocked(false) and
        // clearUserRestriction for the debugging restriction.
        assertTrue(
            DeviceOwnerPolicy.desiredRestrictions(
                isDeviceOwner = true,
                toggleEnabled = false
            ).isEmpty()
        )
    }

    @Test
    fun restrictions_notOwner_appliesNothingEvenWithToggleOn() {
        // Owner status never granted (or lost again): the DPM calls would
        // throw, so the desired set must stay empty in both toggle states.
        assertTrue(
            DeviceOwnerPolicy.desiredRestrictions(
                isDeviceOwner = false,
                toggleEnabled = true
            ).isEmpty()
        )
        assertTrue(
            DeviceOwnerPolicy.desiredRestrictions(
                isDeviceOwner = false,
                toggleEnabled = false
            ).isEmpty()
        )
    }

    // ---- Status precedence ----

    @Test
    fun status_deviceOwnerOutranksProfileOwnerAndAdmin() {
        assertEquals(
            Status.DEVICE_OWNER,
            DeviceOwnerPolicy.statusFor(
                isDeviceOwner = true,
                isProfileOwner = true,
                isAdminActive = true
            )
        )
    }

    @Test
    fun status_profileOwnerOnlyWhenNotDeviceOwner() {
        assertEquals(
            Status.PROFILE_OWNER_ONLY,
            DeviceOwnerPolicy.statusFor(
                isDeviceOwner = false,
                isProfileOwner = true,
                isAdminActive = true
            )
        )
    }

    @Test
    fun status_adminOnlyWhenNoOwnership() {
        assertEquals(
            Status.ADMIN_ONLY,
            DeviceOwnerPolicy.statusFor(
                isDeviceOwner = false,
                isProfileOwner = false,
                isAdminActive = true
            )
        )
    }

    @Test
    fun status_notProvisionedWhenNothingIsActive() {
        assertEquals(
            Status.NOT_PROVISIONED,
            DeviceOwnerPolicy.statusFor(
                isDeviceOwner = false,
                isProfileOwner = false,
                isAdminActive = false
            )
        )
    }

    // ---- Provisioning guidance ----

    @Test
    fun provisioningCommand_isTheExactAdbInvocation() {
        assertEquals(
            "adb shell dpm set-device-owner " +
                "com.aistudio.shieldblock.qxtvnz/" +
                "com.example.deviceowner.ShieldDeviceAdminReceiver",
            DeviceOwnerPolicy.provisioningCommand(
                applicationId = "com.aistudio.shieldblock.qxtvnz",
                receiverClassName = "com.example.deviceowner.ShieldDeviceAdminReceiver"
            )
        )
    }

    @Test
    fun provisioningComponent_flattensIdAndReceiverClass() {
        assertEquals(
            "com.example.app/com.example.deviceowner.ShieldDeviceAdminReceiver",
            DeviceOwnerPolicy.provisioningComponent(
                applicationId = "com.example.app",
                receiverClassName = "com.example.deviceowner.ShieldDeviceAdminReceiver"
            )
        )
    }
}
