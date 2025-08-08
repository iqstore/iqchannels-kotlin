package ru.iqchannels.example.localizations

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.iqchannels.example.R
import ru.iqchannels.sdk.app.IQChannels
import ru.iqchannels.sdk.localization.IQLanguage
import java.io.File
import java.io.FileOutputStream

class LocalizationsEditFragment : Fragment() {
    companion object {
        const val PREFS_LOCALIZATIONS = "LocalizationsEditFragment#prefsLocalizations"
        const val LANGUAGE_CODE = "LanguageCode"

    }

    private lateinit var listView: ListView
    private lateinit var importButton: Button

    private val PICK_FILE_REQUEST_CODE = 1001

    private var selectedLanguageIndex: Int = -1

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_localizations_edit, container, false)

        listView = view.findViewById(R.id.file_list)
        importButton = view.findViewById(R.id.import_button)

        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(Intent.createChooser(intent, "Выберите JSON-файл"), PICK_FILE_REQUEST_CODE)
        }

        updateLocalizationList()
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let {
                val fileName = getFileName(it)
                if (fileName != null) {
                    copyFileToInternalStorage(it, fileName)
                    updateLocalizationList()
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }

    private fun copyFileToInternalStorage(uri: Uri, fileName: String) {
        val destFile = File(requireContext().filesDir, fileName)
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun updateLocalizationList() {
        lifecycleScope.launch {
            val languages = IQChannels.getAvailableLanguages() ?: return@launch

            val languagesName = languages.map { it.Name }

            val adapter = object : ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                languagesName
            ) {}

            listView.adapter = adapter
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE

            val languageCode = requireContext().getSharedPreferences(PREFS_LOCALIZATIONS, Context.MODE_PRIVATE)
                .getString(LANGUAGE_CODE, null)

            if (languageCode == null) {
                selectedLanguageIndex = languages.indexOfFirst { it.Default == true }
            } else {
                selectedLanguageIndex = languages.indexOfFirst { it.Code == languageCode }
            }

            if (selectedLanguageIndex != -1) {
                listView.setItemChecked(selectedLanguageIndex, true)
                onLanguageSelected(languages[selectedLanguageIndex])
            }
            listView.setOnItemClickListener { _, _, position, _ ->
                selectedLanguageIndex = position
                onLanguageSelected(languages[position])
            }
        }
    }

    private fun onLanguageSelected(language: IQLanguage) {
        val code = language.Code
        if (code != null) {
            IQChannels.setLanguage(code)

            requireContext().getSharedPreferences(PREFS_LOCALIZATIONS, Context.MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE_CODE, code)
                .apply()
        }
    }
}