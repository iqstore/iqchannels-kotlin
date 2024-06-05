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

			objBody.setText(JSONObject(configBody).toString(4))

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

			Log.e(javaClass.name, "Error on saving styles", e)
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