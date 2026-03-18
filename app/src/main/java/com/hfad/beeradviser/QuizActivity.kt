package com.hfad.beeradviser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class QuizActivity : AppCompatActivity() {

    private enum class Weather { SUNNY, CLOUD, RAIN }
    private enum class Mood { NICE, OK, SAD }

    private lateinit var quizBackground: ImageView
    private lateinit var startButton: ImageButton
    private lateinit var optionButton1: ImageButton
    private lateinit var optionButton2: ImageButton
    private lateinit var optionButton3: ImageButton
    private lateinit var optionsContainer: View
    private lateinit var resultCardContainer: View
    private lateinit var resultImageView: ImageView
    private lateinit var loadingProgress: ProgressBar

    private val handler = Handler(Looper.getMainLooper())

    private var selectedWeather: Weather? = null
    private var selectedMood: Mood? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        quizBackground = findViewById(R.id.quizBackground)
        startButton = findViewById(R.id.startButton)
        optionButton1 = findViewById(R.id.optionButton1)
        optionButton2 = findViewById(R.id.optionButton2)
        optionButton3 = findViewById(R.id.optionButton3)
        optionsContainer = findViewById(R.id.optionsContainer)
        resultCardContainer = findViewById(R.id.resultCardContainer)
        resultImageView = findViewById(R.id.resultImageView)
        loadingProgress = findViewById(R.id.loadingProgress)

        showStartPage()
    }

    private fun showStartPage() {
        selectedWeather = null
        selectedMood = null

        quizBackground.setImageResource(R.drawable.quiz_page1_background)

        optionsContainer.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultCardContainer.visibility = View.GONE

        startButton.visibility = View.VISIBLE
        startButton.setImageResource(R.drawable.start_quiz)
        startButton.setOnClickListener { showWeatherQuestionPage() }
    }

    private fun showWeatherQuestionPage() {
        // TODO: 改成「第一題背景.png」
        quizBackground.setImageResource(R.drawable.quiz1_background)

        startButton.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultCardContainer.visibility = View.GONE
        optionsContainer.visibility = View.VISIBLE

        optionButton1.setImageResource(R.drawable.sun_option)
        optionButton2.setImageResource(R.drawable.cloud_option)
        optionButton3.setImageResource(R.drawable.rain_option)

        optionButton1.setOnClickListener {
            selectedWeather = Weather.SUNNY
            showMoodQuestionPage()
        }
        optionButton2.setOnClickListener {
            selectedWeather = Weather.CLOUD
            showMoodQuestionPage()
        }
        optionButton3.setOnClickListener {
            selectedWeather = Weather.RAIN
            showMoodQuestionPage()
        }
    }

    private fun showMoodQuestionPage() {
        // TODO: 改成「第二題背景.png」
        quizBackground.setImageResource(R.drawable.quiz2_background)

        startButton.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultCardContainer.visibility = View.GONE
        optionsContainer.visibility = View.VISIBLE


        optionButton1.setImageResource(R.drawable.nice_option)
        optionButton2.setImageResource(R.drawable.ok_option)
        optionButton3.setImageResource(R.drawable.sad_option)

        optionButton1.setOnClickListener {
            selectedMood = Mood.NICE
            showLoadingPage()
        }
        optionButton2.setOnClickListener {
            selectedMood = Mood.OK
            showLoadingPage()
        }
        optionButton3.setOnClickListener {
            selectedMood = Mood.SAD
            showLoadingPage()
        }
    }

    private fun showLoadingPage() {
        quizBackground.setImageResource(R.drawable.quiz_loadingpage)

        startButton.visibility = View.GONE
        optionsContainer.visibility = View.GONE
        resultCardContainer.visibility = View.GONE
        loadingProgress.visibility = View.VISIBLE

        handler.postDelayed({
            showResultPage()
        }, 1200)
    }

    private fun showResultPage() {
        val weather = selectedWeather ?: Weather.SUNNY
        val mood = selectedMood ?: Mood.OK
        val resultDrawable = resultDrawableFor(weather, mood)

        quizBackground.setImageResource(resultBackgroundFor(weather))

        loadingProgress.visibility = View.GONE
        startButton.visibility = View.GONE
        optionsContainer.visibility = View.GONE
        resultCardContainer.visibility = View.VISIBLE
        resultImageView.setImageResource(resultDrawable)
    }

    private fun resultBackgroundFor(weather: Weather): Int {
        val resourceName = when (weather) {
            Weather.SUNNY -> "sun_result_background"
            Weather.CLOUD -> "cloud_result_background"
            Weather.RAIN -> "rain_result_background"
        }

        return resources.getIdentifier(resourceName, "drawable", packageName)
            .takeIf { it != 0 }
            ?: R.drawable.noteimageviewbackground
    }

    private fun resultDrawableFor(weather: Weather, mood: Mood): Int {
        val weatherPrefix = when (weather) {
            Weather.SUNNY -> "sun"
            Weather.CLOUD -> "cloud"
            Weather.RAIN -> "rain"
        }
        val moodPrefix = when (mood) {
            Mood.NICE -> "nice"
            Mood.OK -> "ok"
            Mood.SAD -> "sad"
        }

        val maxPlan = if (weather == Weather.SUNNY) 5 else 4
        val candidateNames = (1..maxPlan).map { index ->
            "${weatherPrefix}${moodPrefix}plan${index}"
        }.shuffled(Random(System.currentTimeMillis()))

        val resolvedResId = candidateNames.firstNotNullOfOrNull { resName ->
            resources.getIdentifier(resName, "drawable", packageName).takeIf { it != 0 }
        }

        return resolvedResId ?: R.drawable.ic_default_plant_placeholder
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
