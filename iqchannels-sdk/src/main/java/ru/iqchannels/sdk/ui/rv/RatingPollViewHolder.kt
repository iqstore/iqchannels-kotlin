package ru.iqchannels.sdk.ui.rv

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.iqchannels.sdk.R
import ru.iqchannels.sdk.schema.PollOptionType
import ru.iqchannels.sdk.schema.Rating
import ru.iqchannels.sdk.schema.RatingPoll
import ru.iqchannels.sdk.schema.RatingPollClientAnswerInput
import ru.iqchannels.sdk.schema.RatingPollQuestion
import ru.iqchannels.sdk.databinding.ChatRatingPollBinding
import ru.iqchannels.sdk.http.HttpCallback
import ru.iqchannels.sdk.schema.RatingState

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
	}

	private fun renderCurrentQuestion() {
		binding.pollOffer.visibility = View.GONE
		binding.submitButton.visibility = View.VISIBLE
		binding.submitButton.isEnabled = false
		poll.Questions?.let {
			if (currentQuestionIndex < it.size) {
				val question = it[currentQuestionIndex]
				renderQuestion(question)
			} else {
				submitPoll()
			}
		}
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

		binding.submitButton.visibility = View.VISIBLE
		binding.submitButton.setOnClickListener {
			if (currentAnswer != null) {
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

		binding.radioGroup.removeAllViews()

		var selectedButton: Button? = null

		question.Answers?.forEach { answer ->
			val button = Button(binding.root.context).apply {
				text = answer.Text
				setBackgroundResource(R.drawable.bg_rating_poll_rounded_button)
				setTextColor(Color.BLACK)

				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					setMargins(0, 10, 0, 10)
				}

				setOnClickListener {
					savePollResult(question.Id, PollOptionType.ONE_OF_LIST, null, answer.Id)

					setBackgroundResource(R.drawable.bg_rating_poll_rounded_button_active)
					setTextColor(Color.WHITE)

					selectedButton?.let { btn ->
						btn.setBackgroundResource(R.drawable.bg_rating_poll_rounded_button)
						btn.setTextColor(Color.BLACK)
					}

					selectedButton = this
				}
			}

			binding.radioGroup.addView(button)
		}
	}


	private fun renderFCRQuestion(question: RatingPollQuestion) {
		binding.pollQuestionFcr.visibility = View.VISIBLE
		binding.yesNoQuestion.text = question.Text

		binding.pollQuestionFcrContainer.removeAllViews()

		var selectedButton: Button? = null

		question.Answers?.forEach { answer ->
			val button = Button(binding.root.context).apply {
				text = answer.Text
				setBackgroundResource(R.drawable.bg_rating_poll_rounded_button)
				setTextColor(Color.BLACK)

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
						answer.FCR
					)

					setBackgroundResource(R.drawable.bg_rating_poll_rounded_button_active)
					setTextColor(Color.WHITE)

					selectedButton?.let { btn ->
						btn.setBackgroundResource(R.drawable.bg_rating_poll_rounded_button)
						btn.setTextColor(Color.BLACK)
					}

					selectedButton = this
				}
			}

			binding.pollQuestionFcrContainer.addView(button)
		}
	}


	private fun renderInputQuestion(question: RatingPollQuestion) {
		binding.pollQuestionInput.visibility = View.VISIBLE
		binding.inputQuestion.text = question.Text

		textWatcher?.let {
			binding.editTextAnswer.removeTextChangedListener(it)
		}

		textWatcher = object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				savePollResult(
					question.Id,
					PollOptionType.INPUT,
					binding.editTextAnswer.text.toString()
				)
			}

			override fun afterTextChanged(s: Editable?) {}
		}

		binding.editTextAnswer.addTextChangedListener(textWatcher)
	}

	private fun renderStarsQuestion(question: RatingPollQuestion) {
		binding.pollQuestionStars.visibility = View.VISIBLE
		binding.starRatingQuestion.text = question.Text

		val starButtons = listOf(
			binding.pollRatingRate1,
			binding.pollRatingRate2,
			binding.pollRatingRate3,
			binding.pollRatingRate4,
			binding.pollRatingRate5
		)

		starButtons.forEachIndexed { index, button ->
			button.setOnClickListener {
				savePollResult(question.Id, PollOptionType.STARS, null, null, (index + 1).toLong())
				updateStarSelection(index)
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
				button.setImageResource(R.drawable.star_filled)
			} else {
				button.setImageResource(R.drawable.star_empty)
			}
		}
	}

	private fun renderScaleQuestion(question: RatingPollQuestion) {
		binding.scaleQuestion.text = question.Text
		binding.pollQuestionScaleContainer.visibility = View.VISIBLE
		binding.pollQuestionScale.removeAllViews()

		val fromValue = question.Scale?.FromValue ?: 0
		val toValue = question.Scale?.ToValue ?: 10
		binding.scaleMinLabel.text = question.Scale?.Items?.get(question.Scale.FromValue)
		binding.scaleMaxLabel.text = question.Scale?.Items?.get(question.Scale.ToValue)
		var selectedButton: Button? = null
		for (i in fromValue..toValue) {
			val button = Button(binding.root.context).apply {
				text = i.toString()
				setBackgroundResource(R.drawable.bg_rating_poll_scale_button)
				includeFontPadding = false
				setTextColor(Color.BLACK)
				setOnClickListener {
					savePollResult(
						question.Id,
						PollOptionType.SCALE,
						null, null, null, i,
					)
					setBackgroundResource(R.drawable.bg_rating_poll_scale_button_active)
					setTextColor(Color.WHITE)
					selectedButton?.let { btn ->
						btn.setBackgroundResource(R.drawable.bg_rating_poll_scale_button)
						btn.setTextColor(Color.BLACK)
					}
					selectedButton = this
				}
				layoutParams = LinearLayout.LayoutParams(
					0,
					100
				).apply {
					setMargins(8, 0, 8, 0)
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
		)
		binding.submitButton.isEnabled = true
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
		listener?.onRatingPollFinished()
	}

	private fun showThanksFeedback() {
		binding.thanksFeedbackLayout.thanksFeedbackText.text = poll.FeedbackThanksText
		binding.thanksFeedbackLayout.root.visibility = View.VISIBLE
	}

	fun setRatingPollListener(listener: RatingPollListener) {
		this.listener = listener
	}
}