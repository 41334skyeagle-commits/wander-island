package com.hfad.beeradviser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class QuizActivity : AppCompatActivity() {

    private enum class Weather { SUNNY, CLOUD, RAIN }
    private enum class Mood { NICE, OK, SAD }

    private lateinit var quizBackground: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var optionButton1: Button
    private lateinit var optionButton2: Button
    private lateinit var optionButton3: Button
    private lateinit var primaryButton: Button
    private lateinit var resultImageView: ImageView
    private lateinit var loadingProgress: ProgressBar

    private val handler = Handler(Looper.getMainLooper())

    private var selectedWeather: Weather? = null
    private var selectedMood: Mood? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        quizBackground = findViewById(R.id.quizBackground)
        titleTextView = findViewById(R.id.titleTextView)
        questionTextView = findViewById(R.id.questionTextView)
        optionButton1 = findViewById(R.id.optionButton1)
        optionButton2 = findViewById(R.id.optionButton2)
        optionButton3 = findViewById(R.id.optionButton3)
        primaryButton = findViewById(R.id.primaryButton)
        resultImageView = findViewById(R.id.resultImageView)
        loadingProgress = findViewById(R.id.loadingProgress)

        showStartPage()
    }

    private fun showStartPage() {
        selectedWeather = null
        selectedMood = null

        quizBackground.setImageResource(R.drawable.island_1_bg)
        titleTextView.text = getString(R.string.quiz_start_title)
        questionTextView.text = getString(R.string.quiz_start_subtitle)

        setOptionsVisible(false)
        loadingProgress.visibility = View.GONE
        resultImageView.visibility = View.GONE

        primaryButton.visibility = View.VISIBLE
        primaryButton.text = getString(R.string.quiz_start_button)
        primaryButton.setOnClickListener { showWeatherQuestionPage() }
    }

    private fun showWeatherQuestionPage() {
        quizBackground.setImageResource(R.drawable.level_2_background)
        titleTextView.text = getString(R.string.quiz_question1_title)
        questionTextView.text = getString(R.string.quiz_question_weather)

        loadingProgress.visibility = View.GONE
        resultImageView.visibility = View.GONE
        primaryButton.visibility = View.GONE

        setupOptions(
            getString(R.string.quiz_option_sunny),
            getString(R.string.quiz_option_cloud),
            getString(R.string.quiz_option_rain)
        ) { index ->
            selectedWeather = when (index) {
                0 -> Weather.SUNNY
                1 -> Weather.CLOUD
                else -> Weather.RAIN
            }
            showMoodQuestionPage()
        }
    }

    private fun showMoodQuestionPage() {
        quizBackground.setImageResource(R.drawable.level_4_background)
        titleTextView.text = getString(R.string.quiz_question2_title)
        questionTextView.text = getString(R.string.quiz_question_mood)

        loadingProgress.visibility = View.GONE
        resultImageView.visibility = View.GONE
        primaryButton.visibility = View.GONE

        setupOptions(
            getString(R.string.quiz_option_nice),
            getString(R.string.quiz_option_ok),
            getString(R.string.quiz_option_sad)
        ) { index ->
            selectedMood = when (index) {
                0 -> Mood.NICE
                1 -> Mood.OK
                else -> Mood.SAD
            }
            showLoadingPage()
        }
    }

    private fun showLoadingPage() {
        quizBackground.setImageResource(R.drawable.level_3_background)
        titleTextView.text = getString(R.string.quiz_loading_title)
        questionTextView.text = getString(R.string.quiz_loading_subtitle)

        setOptionsVisible(false)
        primaryButton.visibility = View.GONE
        resultImageView.visibility = View.GONE
        loadingProgress.visibility = View.VISIBLE

        handler.postDelayed({
            showResultPage()
        }, 1200)
    }

    private fun showResultPage() {
        val weather = selectedWeather ?: Weather.SUNNY
        val mood = selectedMood ?: Mood.OK
        val resultDrawable = resultDrawableFor(weather, mood)

        quizBackground.setImageResource(R.drawable.island_5_bg)
        titleTextView.text = getString(R.string.quiz_result_title)
        questionTextView.text = getString(R.string.quiz_result_subtitle, weather.name, mood.name)

        loadingProgress.visibility = View.GONE
        setOptionsVisible(false)

        resultImageView.visibility = View.VISIBLE
        resultImageView.setImageResource(resultDrawable)

        primaryButton.visibility = View.VISIBLE
        primaryButton.text = getString(R.string.quiz_restart_button)
        primaryButton.setOnClickListener { showStartPage() }
    }

    private fun resultDrawableFor(weather: Weather, mood: Mood): Int {
        return when (weather) {
            Weather.SUNNY -> when (mood) {
                Mood.NICE -> R.drawable.level_1_icon
                Mood.OK -> R.drawable.level_2_icon
                Mood.SAD -> R.drawable.level_3_icon
            }

            Weather.CLOUD -> when (mood) {
                Mood.NICE -> R.drawable.level_2_icon
                Mood.OK -> R.drawable.level_3_icon
                Mood.SAD -> R.drawable.level_4_icon
            }

            Weather.RAIN -> when (mood) {
                Mood.NICE -> R.drawable.level_3_icon
                Mood.OK -> R.drawable.level_4_icon
                Mood.SAD -> R.drawable.level_5_icon
            }
        }
    }

    private fun setupOptions(
        option1: String,
        option2: String,
        option3: String,
        onSelected: (Int) -> Unit
    ) {
        setOptionsVisible(true)
        optionButton1.text = option1
        optionButton2.text = option2
        optionButton3.text = option3

        optionButton1.setOnClickListener { onSelected(0) }
        optionButton2.setOnClickListener { onSelected(1) }
        optionButton3.setOnClickListener { onSelected(2) }
    }

    private fun setOptionsVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        optionButton1.visibility = visibility
        optionButton2.visibility = visibility
        optionButton3.visibility = visibility
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
