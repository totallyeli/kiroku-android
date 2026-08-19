package dev.bugiel.kiroku

import android.app.Application
import dev.bugiel.kiroku.di.AppContainer

class KirokuApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
