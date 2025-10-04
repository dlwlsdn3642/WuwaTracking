package com.jinjinmory.wuwatracking.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProfileRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ProfileRefreshWorker.refresh(context.applicationContext)
            } finally {
                BackgroundRefreshScheduler.scheduleNext(context.applicationContext)
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, ProfileRefreshReceiver::class.java)
    }
}
