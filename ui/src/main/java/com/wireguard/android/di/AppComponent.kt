/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.di

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.wireguard.android.Application
import com.wireguard.android.activity.BaseActivity
import com.wireguard.android.activity.MainActivity
import com.wireguard.android.activity.SettingsActivity.SettingsFragment
import com.wireguard.android.activity.TunnelToggleActivity
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.configStore.ConfigStore
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.di.factory.BackendFactory
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.fragment.BaseFragment
import com.wireguard.android.fragment.ConfigNamingDialogFragment
import com.wireguard.android.fragment.TunnelDetailFragment
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.fragment.TunnelListFragment
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.preference.ToolsInstallerPreference
import com.wireguard.android.preference.VersionPreference
import com.wireguard.android.util.ModuleLoader
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import java9.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }

    // Application
    fun inject(application: Application)

    // Activities
    fun inject(activity: BaseActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: TunnelToggleActivity)

    // Fragments
    fun inject(fragment: BaseFragment)
    fun inject(fragment: AppListDialogFragment)
    fun inject(fragment: ConfigNamingDialogFragment)
    fun inject(fragment: SettingsFragment)
    fun inject(fragment: TunnelDetailFragment)
    fun inject(fragment: TunnelEditorFragment)
    fun inject(fragment: TunnelListFragment)

    // Preferences
    fun inject(preference: ToolsInstallerPreference)
    fun inject(preference: VersionPreference)
    fun inject(receiver: TunnelManager.IntentReceiver)

    // Services
    fun inject(service: GoBackend.VpnService)
}

@Module
object ApplicationModule {
    @get:Singleton
    @get:Provides
    val executor: Executor = AsyncTask.SERIAL_EXECUTOR

    @get:Singleton
    @get:Provides
    val handler: Handler = Handler(Looper.getMainLooper())

    @Singleton
    @Provides
    fun provideSharedPrefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    fun provideConfigStore(context: Context): ConfigStore = FileConfigStore(context)

    @Singleton
    @Provides
    fun provideBackend(
            context: Context,
            moduleLoader: ModuleLoader,
            rootShell: RootShell,
            sharedPreferences: SharedPreferences,
            toolsInstaller: ToolsInstaller,
            tunnelManager: TunnelManager
    ): Backend {
        return BackendFactory.getBackend(
                context,
                moduleLoader,
                rootShell,
                sharedPreferences,
                toolsInstaller,
                tunnelManager
        )
    }

    @Provides
    @Singleton
    fun provideModuleLoader(context: Context, rootShell: RootShell): ModuleLoader {
        return ModuleLoader(context, rootShell, Application.USER_AGENT)
    }

    @Singleton
    @Provides
    fun provideBackendType(backend: Backend): Class<Backend> = backend.javaClass

    @Singleton
    @Provides
    fun provideBackendAsync(backend: Backend): CompletableFuture<Backend> {
        val backendAsync = CompletableFuture<Backend>()
        backendAsync.complete(backend)
        return backendAsync
    }
}
