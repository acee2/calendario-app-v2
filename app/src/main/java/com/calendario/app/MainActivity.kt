package com.calendario.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.calendario.app.databinding.ActivityMainBinding
import com.calendario.app.ui.CalendarFragment
import com.calendario.app.ui.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        askNotificationPermission()
        if (savedInstanceState == null) showCalendar()
        b.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_calendar -> { showCalendar(); true }
                R.id.nav_search -> { showSearch(); true }
                else -> false
            }
        }
    }

    private fun showCalendar() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHost, CalendarFragment()).commit()
    }

    private fun showSearch() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHost, SearchFragment()).commit()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
            )
        }
    }
}
