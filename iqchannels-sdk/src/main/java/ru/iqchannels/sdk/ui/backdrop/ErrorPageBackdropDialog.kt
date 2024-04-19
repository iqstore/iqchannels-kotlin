package ru.iqchannels.sdk.ui.backdrop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.databinding.BackdropErrorPageBinding

class ErrorPageBackdropDialog : BottomSheetDialogFragment() {

	companion object {
		private const val ARG_DESCRIPTION = "ErrorPageBackdropDialog#description"
		const val TRANSACTION_TAG = "ErrorPageBackdropDialog#transactionTag"

		fun newInstance(description: String?): ErrorPageBackdropDialog {
			val fragment = ErrorPageBackdropDialog()
			val args = Bundle()
			args.putString(ARG_DESCRIPTION, description)
			fragment.arguments = args

			return fragment
		}
	}

	override fun getTheme(): Int {
		return R.style.Theme_Dialog_BottomSheet
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		super.onCreateView(inflater, container, savedInstanceState)

		val binding = BackdropErrorPageBinding.inflate(inflater, container, false)

		binding.run {
			tvTitle.text = getString(R.string.file_not_uploaded)
			tvDescription.text = requireArguments().getString(ARG_DESCRIPTION)

			btnAction.setOnClickListener {
				dismiss()
			}
		}

		return binding.root
	}
}