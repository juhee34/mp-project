package com.tinyrunners.school

import android.app.Application
import com.tinyrunners.school.audio.MusicLifecycle

/** Starts the app-wide pieces: music that follows whichever screen is showing. */
class TinyRunnersApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(MusicLifecycle())
    }
}
