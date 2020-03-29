/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.di.factory

import android.content.Context
import android.content.SharedPreferences
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.ModuleLoader
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller

object BackendFactory {
    fun getBackend(
            context: Context,
            moduleLoader: ModuleLoader,
            rootShell: RootShell,
            sharedPreferences: SharedPreferences,
            toolsInstaller: ToolsInstaller,
            tunnelManager: TunnelManager
    ): Backend {
        var ret: Backend? = null
        var didStartRootShell = false
        if (!ModuleLoader.isModuleLoaded() && moduleLoader.moduleMightExist()) {
            try {
                rootShell.start()
                didStartRootShell = true
                moduleLoader.loadModule()
            } catch (_: Exception) {
            }
        }
        if (!sharedPreferences.getBoolean("disable_kernel_module", false) && ModuleLoader.isModuleLoaded()) {
            try {
                if (!didStartRootShell) rootShell.start()
                ret = WgQuickBackend(context, rootShell, toolsInstaller).apply {
                    setMultipleTunnels(sharedPreferences.getBoolean("multiple_tunnels", false))
                }
            } catch (_: Exception) {
            }
        }
        if (ret == null) {
            ret = GoBackend(context)
            GoBackend.setAlwaysOnCallback {
                tunnelManager.restoreState(true).whenComplete(ExceptionLoggers.D)
            }
        }
        return ret
    }
}
