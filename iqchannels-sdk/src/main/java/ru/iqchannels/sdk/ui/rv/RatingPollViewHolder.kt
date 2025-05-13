package ru.iqchannels.sdk.ui.rv

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.applyIQStyles
import ru.iqchannels.sdk.schema.PollOptionType
import ru.iqchannels.sdk.schema.Rating
import ru.iqchannels.sdk.schema.RatingPoll
import ru.iqchannels.sdk.schema.RatingPollClientAnswerInput
import ru.iqchannels.sdk.schema.RatingPollQuestion
import ru.iqchannels.sdk.databinding.ChatRatingPollBinding
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.schema.RatingState
import ru.iqchannels.sdk.setBackgroundDrawable
import ru.iqchannels.sdk.styling.IQStyles
import ru.iqchannels.sdk.ui.UiUtils.toPx

internal class RatingPollViewHolder(
	private val binding: ChatRatingPollBinding
) : ViewHolder(binding.root) {

	private var pollResult: MutableList<RatingPollClientAnswerInput> = mutableListOf()
	private var currentQuestionIndex: Int = 0
	private var currentAnswer: RatingPollClientAnswerInput? = null
	private lateinit var rating: Rating
	private lateinit var poll: RatingPoll
	private var listener: RatingPollListener? = null
	private var textWatcher: TextWatcher? = null

	fun bindPoll(ratingPoll: RatingPoll, rating: Rating) {
		this.rating = rating
		this.poll = ratingPoll
		if (ratingPoll.ShowOffer) {
			showOffer()
		} else {
			renderCurrentQuestion()
		}
	}

	private fun showOffer() {
		binding.pollOffer.visibility = View.VISIBLE
		binding.submitButton.visibility = View.GONE
		hideAllQuestionLayouts()

		binding.buttonYes.setOnClickListener {
			renderCurrentQuestion()
		}

		binding.buttonNo.setOnClickListener {
			finishPoll()
		}


		binding.questionText.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)

		IQStyles.iqChannelsStyles?.rating?.backgroundContainer
			?.let {
				binding.pollBackground.setBackgroundDrawable(it, R.drawable.other_msg_bg)
			}

		IQStyles.iqChannelsStyles?.rating?.sentRating?.backgroundEnabled
			?.let {
				binding.buttonYes.setBackgroundDrawable(it, R.drawable.bg_button_rate)
			}
		binding.buttonYes.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.sentRating?.textEnabled)

		IQStyles.iqChannelsStyles?.rating?.sentRating?.backgroundDisabled
			?.let {
				binding.buttonNo.setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
			}
		binding.buttonNo.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.sentRating?.textDisabled)
	}

	private fun renderCurrentQuestion() {
		binding.pollOffer.visibility = View.GONE
//		binding.submitButton.visibility = View.VISIBLE
		binding.submitButton.isEnabled = false
		poll.Questions?.let {
			if (currentQuestionIndex < it.size) {
				val question = it[currentQuestionIndex]
				renderQuestion(question)
			} else {
				submitPoll()
			}
		}

		IQStyles.iqChannelsStyles?.rating?.sentRating?.backgroundDisabled
			?.let {
				binding.submitButton.setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
			}
		binding.submitButton.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.sentRating?.textDisabled)

		Handler(Looper.getMainLooper()).postDelayed({
			listener?.onRatingRenderQuestion()
		}, 30)
	}

	private fun hideAllQuestionLayouts() {
		binding.pollQuestionOneOfList.visibility = View.GONE
		binding.pollQuestionFcr.visibility = View.GONE
		binding.pollQuestionInput.visibility = View.GONE
		binding.pollQuestionStars.visibility = View.GONE
		binding.pollQuestionScaleContainer.visibility = View.GONE
	}

	private fun renderQuestion(question: RatingPollQuestion) {
		hideAllQuestionLayouts()

		when (question.Type) {
			PollOptionType.ONE_OF_LIST -> {
				renderOneOfListQuestion(question)
			}

			PollOptionType.FCR -> {
				renderFCRQuestion(question)
			}

			PollOptionType.INPUT -> {
				renderInputQuestion(question)
			}

			PollOptionType.STARS -> {
				renderStarsQuestion(question)
			}

			PollOptionType.SCALE -> {
				renderScaleQuestion(question)
			}
		}

		if(question.Type == PollOptionType.FCR || question.Type == PollOptionType.ONE_OF_LIST) {
			binding.submitButton.visibility = View.GONE
		}else{
			binding.submitButton.visibility = View.VISIBLE
		}
		binding.submitButton.setOnClickListener {
			if (currentAnswer != null) {
				if (currentAnswer!!.AsTicketRating == true) {
					if (currentAnswer!!.Type == PollOptionType.STARS) {
						rating.Value = currentAnswer!!.AnswerStars?.toInt()
					}
					if (currentAnswer!!.Type == PollOptionType.SCALE) {
						rating.Value = currentAnswer!!.AnswerScale
					}
				}
				currentQuestionIndex++
				renderCurrentQuestion()
				pollResult.add(currentAnswer!!)
				currentAnswer = null
			}
		}
	}

	private fun renderOneOfListQuestion(question: RatingPollQuestion) {
		binding.pollQuestionOneOfList.visibility = View.VISIBLE
		binding.singleChoiceQuestion.text = question.Text
		binding.singleChoiceQuestion.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)

		binding.radioGroup.removeAllViews()

		question.Answers?.forEach { answer ->
			IQStyles.iqChannelsStyles?.rating?.sentRating?.backgroundEnabled
				?.let {
					binding.buttonYes.setBackgroundDrawable(it, R.drawable.bg_button_rate)
				}
			binding.buttonYes.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.sentRating?.textEnabled)

			IQStyles.iqChannelsStyles?.rating?.sentRating?.backgroundDisabled
				?.let {
					binding.buttonNo.setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
				}
			binding.buttonNo.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.sentRating?.textDisabled)


			val button = Button(binding.root.context).apply {
				text = answer.Text

				IQStyles.iqChannelsStyles?.rating?.answerButton?.backgroundDisabled
					?.let {
						setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
					}
				applyIQStyles(IQStyles.iqChannelsStyles?.rating?.answerButton?.textDisabled)

				elevation = 0f
				stateListAnimator = null
				isAllCaps = false

				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					setMargins(0, 10, 0, 10)
				}

				setOnClickListener {
					savePollResult(
						question.Id,
						PollOptionType.ONE_OF_LIST, null, answer.Id,
						null, null, null, question.AsTicketRating,
					)
					currentQuestionIndex++
					renderCurrentQuestion()
					pollResult.add(currentAnswer!!)
					currentAnswer = null
				}
			}

			binding.radioGroup.addView(button)
		}
	}


	private fun renderFCRQuestion(question: RatingPollQuestion) {
		binding.pollQuestionFcr.visibility = View.VISIBLE
		binding.yesNoQuestion.text = question.Text
		binding.yesNoQuestion.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)

		binding.pollQuestionFcrContainer.removeAllViews()

		question.Answers?.forEach { answer ->
			val button = Button(binding.root.context).apply {
				text = answer.Text

				IQStyles.iqChannelsStyles?.rating?.answerButton?.backgroundDisabled
					?.let {
						setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
					}
				applyIQStyles(IQStyles.iqChannelsStyles?.rating?.answerButton?.textDisabled)

				elevation = 0f
				stateListAnimator = null

				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					setMargins(15, 0, 15, 0)
				}

				setOnClickListener {
					savePollResult(
						question.Id,
						PollOptionType.FCR,
						null,
						answer.Id,
						null,
						null,
						answer.FCR,
						question.AsTicketRating,
					)
					currentQuestionIndex++
					renderCurrentQuestion()
					pollResult.add(currentAnswer!!)
					currentAnswer = null
				}
			}

			binding.pollQuestionFcrContainer.addView(button)
		}
	}


	private fun renderInputQuestion(question: RatingPollQuestion) {
		binding.pollQuestionInput.visibility = View.VISIBLE
		binding.inputQuestion.text = question.Text
		binding.inputQuestion.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)

		textWatcher?.let {
			binding.editTextAnswer.removeTextChangedListener(it)
		}

		textWatcher = object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				savePollResult(
					question.Id,
					PollOptionType.INPUT,
					binding.editTextAnswer.text.toString(),
					null, null, null, null,
					question.AsTicketRating,
				)
			}

			override fun afterTextChanged(s: Editable?) {}
		}

		binding.editTextAnswer.addTextChangedListener(textWatcher)


		IQStyles.iqChannelsStyles?.rating?.inputBackground
			?.let {
				binding.editTextAnswer.setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_edit_text)
			}
		binding.editTextAnswer.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.inputText)


	}

	private fun renderStarsQuestion(question: RatingPollQuestion) {
		binding.pollQuestionStars.visibility = View.VISIBLE
		binding.starRatingQuestion.text = question.Text
		binding.starRatingQuestion.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)

		val starButtons = listOf(
			binding.pollRatingRate1,
			binding.pollRatingRate2,
			binding.pollRatingRate3,
			binding.pollRatingRate4,
			binding.pollRatingRate5
		)

		starButtons.forEachIndexed { index, button ->
			button.setOnClickListener {
				savePollResult(
					question.Id, PollOptionType.STARS, null, null, (index + 1).toLong(),
					null, null, question.AsTicketRating,
				)
				updateStarSelection(index)
			}

			IQStyles.iqChannelsStyles?.rating?.emptyStar?.let {
				Glide.with(binding.root.context)
					.load(it)
					.into(button)
			} ?: run {
				button.setImageResource(R.drawable.star_empty)
			}
		}
	}

	private fun updateStarSelection(selectedIndex: Int) {
		val starButtons = listOf(
			binding.pollRatingRate1,
			binding.pollRatingRate2,
			binding.pollRatingRate3,
			binding.pollRatingRate4,
			binding.pollRatingRate5
		)

		starButtons.forEachIndexed { index, button ->
			if (index <= selectedIndex) {
				IQStyles.iqChannelsStyles?.rating?.fullStar?.let {
					Glide.with(binding.root.context)
						.load(it)
						.into(button)
				} ?: run {
					button.setImageResource(R.drawable.star_filled)
				}

//				button.setImageResource(R.drawable.star_filled)
			} else {
				IQStyles.iqChannelsStyles?.rating?.emptyStar?.let {
					Glide.with(binding.root.context)
						.load(it)
						.into(button)
				} ?: run {
					button.setImageResource(R.drawable.star_empty)
				}
//				button.setImageResource(R.drawable.star_empty)
			}
		}
	}

	private fun renderScaleQuestion(question: RatingPollQuestion) {
		binding.scaleQuestion.text = question.Text
		binding.scaleQuestion.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.ratingTitle)
		binding.pollQuestionScaleContainer.visibility = View.VISIBLE
		binding.pollQuestionScale.removeAllViews()

		val fromValue = question.Scale?.FromValue ?: 0
		val toValue = question.Scale?.ToValue ?: 10
		binding.scaleMinLabel.text = question.Scale?.Items?.get(question.Scale.FromValue)
		binding.scaleMinLabel.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.scaleMinText)
		binding.scaleMaxLabel.text = question.Scale?.Items?.get(question.Scale.ToValue)
		binding.scaleMaxLabel.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.scaleMaxText)
		var selectedButton: Button? = null
		for (i in fromValue..toValue) {
			val button = Button(binding.root.context).apply {
				text = i.toString()

				IQStyles.iqChannelsStyles?.rating?.scaleButton?.backgroundDisabled
					?.let {
						setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
					}
				applyIQStyles(IQStyles.iqChannelsStyles?.rating?.scaleButton?.textDisabled)

				includeFontPadding = false
				elevation = 0f
				stateListAnimator = null
				setOnClickListener {
					savePollResult(
						question.Id,
						PollOptionType.SCALE,
						null, null, null, i, null, question.AsTicketRating,
					)

					IQStyles.iqChannelsStyles?.rating?.scaleButton?.backgroundEnabled
						?.let {
							setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button_active)
						}
					applyIQStyles(IQStyles.iqChannelsStyles?.rating?.scaleButton?.textEnabled)

					selectedButton?.let { btn ->
						IQStyles.iqChannelsStyles?.rating?.scaleButton?.backgroundDisabled
							?.let {
								btn.setBackgroundDrawable(it, R.drawable.bg_rating_poll_rounded_button)
							}
						btn.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.scaleButton?.textDisabled)
					}

					selectedButton = this
				}
				layoutParams = LinearLayout.LayoutParams(
					0,
					toPx(42)
				).apply {
					setPadding(15)
					weight = 1f
				}
			}
			binding.pollQuestionScale.addView(button)
		}
	}

	private fun savePollResult(
		questionId: Long,
		type: PollOptionType,
		input: String? = null,
		answerId: Long? = null,
		stars: Long? = null,
		scaleValue: Int? = null,
		fcr: Boolean? = null,
		asTicketRating: Boolean? = null,
	) {
		currentAnswer = RatingPollClientAnswerInput(
			rating.ProjectId,
			rating.ClientId,
			rating.Id,
			questionId,
			type,
			answerId,
			input,
			stars,
			scaleValue,
			fcr,
			asTicketRating,
		)
		binding.submitButton.isEnabled = true

		IQStyles.iqChannelsStyles?.rating?.sentRating?.backgroundEnabled
			?.let {
				binding.submitButton.setBackgroundDrawable(it, R.drawable.bg_button_rate)
			}
		binding.submitButton.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.sentRating?.textEnabled)
	}

	private fun submitPoll() {
		listener?.onRatingPollAnswersSend(
			this.pollResult,
			this.rating.Id,
			this.poll.Id,
			object : HttpCallback<Void> {
				override fun onResult(result: Void?) {
					binding.root.post {
						finishPoll()
					}
				}

				override fun onException(exception: Exception) {}
			}
		)
	}

	private fun finishPoll() {
		hideAllQuestionLayouts()

		if (poll.FeedbackThanks) {
			binding.submitButton.visibility = View.GONE
			showThanksFeedback()
			binding.root.postDelayed({
				hidePoll()
			}, 2000)
		} else {
			binding.root.post {
				hidePoll()
			}
		}
	}

	private fun hidePoll() {
		binding.thanksFeedbackLayout.root.visibility = View.GONE
		binding.root.visibility = View.GONE
		rating.State = RatingState.FINISHED
		listener?.onRatingPollFinished(rating.Value)
		listener?.onRatingRenderQuestion()
	}

	private fun showThanksFeedback() {
		binding.thanksFeedbackLayout.thanksFeedbackText.text = poll.FeedbackThanksText
		binding.thanksFeedbackLayout.root.visibility = View.VISIBLE
		binding.thanksFeedbackLayout.thanksFeedbackText.applyIQStyles(IQStyles.iqChannelsStyles?.rating?.feedbackThanksText)
	}

	fun setRatingPollListener(listener: RatingPollListener) {
		this.listener = listener
	}
}