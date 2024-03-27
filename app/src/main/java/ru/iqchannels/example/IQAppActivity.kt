/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.example

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.installations.FirebaseInstallations
import ru.iqchannels.sdk.app.Cancellable
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsConfig
import ru.iqchannels.sdk.app.UnreadListener
import ru.iqchannels.sdk.ui.ChatFragment

class IQAppActivity :
	AppCompatActivity(),
	NavigationView.OnNavigationItemSelectedListener,
	UnreadListener {

	companion object {
		private const val TAG = "iqchannels-app"
	}

	private var unread: Cancellable? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_app)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
		val toggle = ActionBarDrawerToggle(
			this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
		)
		drawer.addDrawerListener(toggle)
		toggle.syncState()
		val navigationView = findViewById<NavigationView>(R.id.nav_view)
		navigationView.setNavigationItemSelectedListener(this)
		setupIQChannels()
	}

	private fun setupIQChannels() {
		FirebaseInstallations.getInstance().getToken(false)
			.addOnCompleteListener(OnCompleteListener { task ->
				if (!task.isSuccessful) {
					return@OnCompleteListener
				}

				// Get new Instance ID token.
				val token = task.result!!.token
				val iq = IQChannels
				iq.setPushToken(token)
			})
		val iq = IQChannels
		// iq.configure(this, new IQChannelsConfig("http://52.57.77.143/", "support"));
		iq.configure(this, IQChannelsConfig("https://sandbox.iqstore.ru/", "support"))
		iq.loginAnonymous()
		// iq.configure(this, new IQChannelsConfig("http://88.99.143.201/", "support"));
	}

	override fun onBackPressed() {
		val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START)
		} else {
			super.onBackPressed()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.app, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		val id = item.itemId
		return if (id == R.id.action_settings) {
			true
		} else super.onOptionsItemSelected(item)
	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		val fragment: Fragment
		when (item.itemId) {
			R.id.nav_index -> fragment = PlusOneFragment.newInstance()
			R.id.nav_chat -> fragment = ChatFragment.newInstance()
			R.id.nav_login -> {
				IQChannels.login("3")
				return false
			}

			R.id.nav_logout -> {
				IQChannels.logout()
				IQChannels.loginAnonymous()
				return false
			}

			R.id.nav_logout_anonymous -> {
				IQChannels.logoutAnonymous()
				return false
			}

			R.id.nav_listen_to_unread -> {
				if (unread == null) {
					unread = IQChannels.addUnreadListener(this)
				}
				return false
			}

			R.id.nav_remove_unread_listener -> {
				if (unread != null) {
					unread!!.cancel()
					unread = null
				}
				return false
			}

			else -> return false
		}

		// Insert the fragment by replacing any existing fragment
		val fragmentManager = supportFragmentManager
		fragmentManager.beginTransaction().replace(R.id.content, fragment).commit()
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		drawer.closeDrawer(GravityCompat.START)
		return true
	}

	override fun unreadChanged(unread: Int) {
		Log.i(TAG, String.format("Unread: %d", unread))
		val nav = findViewById<NavigationView>(R.id.nav_view)
		val menu = nav.menu
		val item = menu.findItem(R.id.nav_chat)
		if (unread == 0) {
			item.setTitle("Chat")
		} else {
			item.setTitle(String.format("Chat (%d)", unread))
		}
	}
}
