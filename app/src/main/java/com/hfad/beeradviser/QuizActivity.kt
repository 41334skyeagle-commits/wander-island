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
        resultImageView = findViewById(R.id.resultImageView)
        loadingProgress = findViewById(R.id.loadingProgress)

        showStartPage()
    }

    private fun showStartPage() {
        selectedWeather = null
        selectedMood = null

        // TODO: 改成「開始頁背景.png」
        quizBackground.setImageResource(R.drawable.island_1_bg)

        optionsContainer.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultImageView.visibility = View.GONE

        startButton.visibility = View.VISIBLE
        // TODO: 改成「開始測驗按鈕.png」
        startButton.setImageResource(R.drawable.ic_button5)
        startButton.setOnClickListener { showWeatherQuestionPage() }
    }

    private fun showWeatherQuestionPage() {
        // TODO: 改成「第一題背景.png」
        quizBackground.setImageResource(R.drawable.level_2_background)

        startButton.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultImageView.visibility = View.GONE
        optionsContainer.visibility = View.VISIBLE

        // TODO: 改成 sunny/cloud/rain 的按鈕圖
        optionButton1.setImageResource(R.drawable.ic_button1)
        optionButton2.setImageResource(R.drawable.ic_button2)
        optionButton3.setImageResource(R.drawable.ic_button3)

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
        quizBackground.setImageResource(R.drawable.level_4_background)

        startButton.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultImageView.visibility = View.GONE
        optionsContainer.visibility = View.VISIBLE

        // TODO: 改成 nice/ok/sad 的按鈕圖
        optionButton1.setImageResource(R.drawable.ic_button1)
        optionButton2.setImageResource(R.drawable.ic_button2)
        optionButton3.setImageResource(R.drawable.ic_button3)

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
        // TODO: 改成「過渡頁面.png」
        quizBackground.setImageResource(R.drawable.level_3_background)

        startButton.visibility = View.GONE
        optionsContainer.visibility = View.GONE
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

        // TODO: 改成結果頁背景（若需要）
        quizBackground.setImageResource(R.drawable.island_5_bg)

        loadingProgress.visibility = View.GONE
        startButton.visibility = View.GONE
        optionsContainer.visibility = View.GONE

        resultImageView.visibility = View.VISIBLE
        resultImageView.setImageResource(resultDrawable)
    }

    private fun resultDrawableFor(weather: Weather, mood: Mood): Int {
        val weatherPrefix = when (weather) {
            Weather.SUNNY -> "sunny"
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
