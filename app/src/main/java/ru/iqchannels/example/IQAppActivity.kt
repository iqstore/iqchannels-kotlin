/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */
package ru.iqchannels.example

import android.content.Context
import android.os.Bundle
import ru.iqchannels.sdk.Log
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
import com.google.firebase.messaging.FirebaseMessaging
import ru.iqchannels.example.styles.StylesEditFragment
import ru.iqchannels.sdk.app.Cancellable
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsConfig
import ru.iqchannels.sdk.app.IQChannelsConfig2
import ru.iqchannels.sdk.app.IQChannelsFactory
import ru.iqchannels.sdk.app.UIOptions
import ru.iqchannels.sdk.app.UnreadListener
import ru.iqchannels.sdk.ui.ChatFragment
import ru.iqchannels.sdk.ui.channels.ChannelsFragment

class IQAppActivity :
	AppCompatActivity(),
	NavigationView.OnNavigationItemSelectedListener,
	UnreadListener {

	companion object {
		private const val TAG = "iqchannels-app"

		const val PREFS = "IQAppActivity#prefs"
		const val TESTING_TYPE = "IQAppActivity#testingType"
		const val ADDRESS = "IQAppActivity#address"
		const val CHANNELS = "IQAppActivity#channels"
	}

	private var unread: Cancellable? = null

	private var customNavBarEnabled = false

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
		//setStylesPrefs()
	}

	private fun setupIQChannels() {
		FirebaseMessaging.getInstance().token
			.addOnCompleteListener(OnCompleteListener { task ->
				if (!task.isSuccessful) {
					return@OnCompleteListener
				}

				// Get new Instance ID token.
				val token = task.result
				val iq = IQChannels
				iq.setPushToken(token)
			})

		val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
		val testingType = prefs
			.getString(TESTING_TYPE, TestingType.MultiChat.name)?.let {
				TestingType.valueOf(it)
			}

		val address = prefs.getString(ADDRESS, null) ?: "https://sandbox.iqstore.ru"
		val channels = prefs.getStringSet(CHANNELS, null) ?: setOf("support", "finance")

		when (testingType) {
			TestingType.SingleChat -> {
				IQChannels.configure(
					this,
					IQChannelsConfig(address, channels.first(), true, UIOptions(true))
				)
				IQChannels.loginAnonymous()
				IQChannels.getSignupGreetingSettings()
			}

			else -> Unit
		}
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
			R.id.nav_index -> fragment = MainFragment()
			R.id.nav_chat -> {
				val stylesJson = getSharedPreferences(StylesEditFragment.PREFS_STYLES, Context.MODE_PRIVATE)
					.getString(StylesEditFragment.CONFIG_STYLES, null)
				fragment = ChatFragment.newInstance(stylesJson = stylesJson)
			}
			R.id.nav_login -> {
				val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
				val testingType = prefs
					.getString(TESTING_TYPE, TestingType.MultiChat.name)?.let {
						TestingType.valueOf(it)
					}

				val address = prefs.getString(ADDRESS, null) ?: "https://sandbox.iqstore.ru"
				val channels = prefs.getStringSet(CHANNELS, null) ?: setOf("support", "finance")

				when(testingType) {
					TestingType.SingleChat -> {
						IQChannels.login("3")
					}
					TestingType.MultiChat -> {
						IQChannelsFactory().create(
							context = this,
							config = IQChannelsConfig2(
								address = address,
								channels = channels.toList()
							),
							credentials = "3"
						)
					}
					null -> Unit
				}
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

			R.id.send_huawei_token -> {
				IQChannels.setPushToken("test", isHuawei = true)
				return false
			}

			R.id.nav_multi_chat -> {
				fragment = ChannelsFragment.newInstance(customNavBarEnabled)
			}

			R.id.hide_action_bar -> {
				supportActionBar?.hide()
				return false
			}

			R.id.show_action_bar -> {
				supportActionBar?.show()
				return false
			}

			R.id.enable_cutom_title -> {
				customNavBarEnabled = !customNavBarEnabled
				return false
			}

			else -> return false
		}

		// Insert the fragment by replacing any existing fragment
		val fragmentManager = supportFragmentManager
		fragmentManager.beginTransaction().replace(R.id.content, fragment).addToBackStack(null)
			.commit()
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

	private fun setStylesPrefs() {
		val prefs = getSharedPreferences(StylesEditFragment.PREFS_STYLES, Context.MODE_PRIVATE)

		if (prefs.getString(StylesEditFragment.CONFIG_STYLES, null) == null) {
			val stylesJson = getJSFromAssets("styles_new.json")?.toString()
			prefs.edit()
				.putString(StylesEditFragment.CONFIG_STYLES, stylesJson)
				.apply()
		}
	}
}
