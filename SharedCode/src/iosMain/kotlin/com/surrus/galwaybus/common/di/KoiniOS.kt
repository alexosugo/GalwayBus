package com.surrus.galwaybus.common.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.surrus.galwaybus.db.MyDatabase
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
    single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single { createDb() }
}


fun createDb(): MyDatabase {
    val driver = NativeSqliteDriver(MyDatabase.Schema, "galwaybus.db")
    return MyDatabase(driver)
}