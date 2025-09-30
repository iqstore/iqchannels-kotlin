package ru.iqchannels.example.styles

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException
import kotlin.system.exitProcess
import org.json.JSONObject
import ru.iqchannels.example.IQAppActivity
import ru.iqchannels.example.databinding.FragmentStylesEditBinding
import ru.iqchannels.sdk.Log

class StylesEditFragment : Fragment() {
	companion object {
		const val PREFS_STYLES = "StylesEditFragment#prefsStyles"
		const val CONFIG_STYLES = "configStyles"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)

		val configBody = requireContext().getSharedPreferences(PREFS_STYLES, Context.MODE_PRIVATE)
			.getString(CONFIG_STYLES, null)

		val binding = FragmentStylesEditBinding.inflate(inflater, container, false).apply {

			val json = runCatching {
				JSONObject(configBody).toString(4)
			}.getOrNull()

			objBody.setText(json)

			applyChanges.setOnClickListener {
				objBody.text?.toString()?.let { it1 -> apply(it1) }
			}
		}

		return binding.root
	}

	private fun apply(itemBody: String) {
		try {
			JSONObject(itemBody).also {
				requireContext().getSharedPreferences(PREFS_STYLES, Context.MODE_PRIVATE)
					.edit()
					.putString(CONFIG_STYLES, it.toString())
					.apply()
			}

			Snackbar.make(
				requireView(),
				"Saved",
				Snackbar.LENGTH_SHORT
			).apply {
				setAction("Restart") {
					restartAppToApply()
				}
				show()
			}
		} catch (e: Exception) {
			Toast.makeText(
				requireContext(),
				"Error!",
				Toast.LENGTH_SHORT
			).show()

			if (e is JSONException) {
				val pos = Regex("character (\\d+)")
					.find(e.message ?: "")
					?.groupValues
					?.getOrNull(1)
					?.toIntOrNull()

				if (pos != null) {
					var line = 0
					for (i in 0 until pos) {
						if (itemBody[i] == '\n') {
							line++
						}
					}

					val start = (pos - 40).coerceAtLeast(0)
					val end = (pos + 40).coerceAtMost(itemBody.length)
					val context = itemBody.substring(start, end)

					Log.e(
						"Styles",
						"JSON parse error at line $line, char $pos: ...$context..."
					)
				}
			}
		}
	}

	private fun restartAppToApply() {
		activity?.finish()
		val intent = Intent(context?.applicationContext, IQAppActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context?.startActivity(intent)
		exitProcess(0)
	}
}