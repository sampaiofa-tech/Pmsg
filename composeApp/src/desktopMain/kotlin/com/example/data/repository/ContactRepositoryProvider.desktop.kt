package com.example.data.repository

actual object ContactRepositoryProvider {
    private val instance by lazy { DesktopContactRepository() }

    actual fun get(): ContactRepository = instance
}
