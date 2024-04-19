package ru.iqchannels.sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.runs
import java.lang.reflect.Method
import org.junit.Before
import org.junit.Test
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.app.IQChannelsConfig
import ru.iqchannels.sdk.http.HttpClient
import ru.iqchannels.sdk.http.HttpRequest
import ru.iqchannels.sdk.schema.Client
import ru.iqchannels.sdk.schema.ClientAuth
import ru.iqchannels.sdk.schema.ClientSession

class IQChannelsTest {

	companion object {

		const val CHANNEL = "support"
		const val ADDRESS = "https://iqchannels.isimplelab.com"
		const val TOKEN = "test_token"
		const val ANONYMOUS_TOKEN = "anonymous_token"
	}

	@MockK
	private lateinit var httpClient: HttpClient

	@MockK
	private lateinit var httpRequest: HttpRequest

	@MockK
	private lateinit var sharedPreferences: SharedPreferences

	@MockK
	private lateinit var context: Context

	private val clientAuth = ClientAuth().apply {
		Client = Client().apply {
			Name = "UserTest"
			Id = 1
		}

		Session = ClientSession().apply {
			Token = TOKEN
		}
	}

	@Before
	fun setUp() {
		MockKAnnotations.init(this)

		every { context.applicationContext.mainLooper } returns mockkClass(Looper::class)
		every { context.applicationContext.getSharedPreferences(any(), any()) } returns sharedPreferences

		IQChannels.configure(
			context = context,
			config = IQChannelsConfig(ADDRESS, CHANNEL)
		)
	}

	@Test
	fun loginSuccessTest() {

		//given
		val credentials = "3"

		every { httpClient.clientsIntegrationAuth(credentials, CHANNEL, any()) } returns httpRequest
		mockkStatic(Log::class)
		every { Log.d(any(), any()) } returns 0

		//when
		IQChannels.login(credentials)

		val authCompleteMethod: Method = IQChannels::class.java.getDeclaredMethod("authComplete", ClientAuth::class.java)
		authCompleteMethod.isAccessible = true

		authCompleteMethod.invoke(IQChannels, clientAuth)

		//then
		assert(IQChannels.auth == clientAuth)
	}

	@Test
	fun loginAnonymousSuccessTest() {

		//given
		every { httpClient.clientsAuth(any(), any()) } returns httpRequest
		mockkStatic(Log::class)
		every { Log.d(any(), any()) } returns 0
		every { sharedPreferences.getString(ANONYMOUS_TOKEN, any()) } returns TOKEN

		//when
		IQChannels.loginAnonymous()

		val authCompleteMethod: Method = IQChannels::class.java.getDeclaredMethod("authComplete", ClientAuth::class.java)
		authCompleteMethod.isAccessible = true

		authCompleteMethod.invoke(IQChannels, clientAuth)

		//then
		assert(IQChannels.auth == clientAuth)
	}
}