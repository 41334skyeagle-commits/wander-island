package com.hfad.beeradviser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class QuizActivity : AppCompatActivity() {

    private enum class Weather { SUNNY, CLOUD, RAIN }
    private enum class Mood { NICE, OK, SAD }

    private lateinit var quizBackground: ImageView
    private lateinit var startButtonContainer: View
    private lateinit var startButton: ImageButton
    private lateinit var startButtonShimmerRay: View
    private lateinit var optionButton1: ImageButton
    private lateinit var optionButton2: ImageButton
    private lateinit var optionButton3: ImageButton
    private lateinit var optionsContainer: View
    private lateinit var resultCardContainer: View
    private lateinit var resultImageView: ImageView
    private lateinit var resultExitButton: ImageButton
    private lateinit var loadingProgress: ProgressBar

    private val handler = Handler(Looper.getMainLooper())
    private var shimmerAnimator: ObjectAnimator? = null

    private var selectedWeather: Weather? = null
    private var selectedMood: Mood? = null

    companion object {
        const val EXTRA_ENTRY_SOURCE = "quiz_entry_source"
        const val EXTRA_LEVEL_ID = "quiz_level_id"
        const val ENTRY_MAIN = "main"
        const val ENTRY_LEVEL = "level"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        quizBackground = findViewById(R.id.quizBackground)
        startButtonContainer = findViewById(R.id.startButtonContainer)
        startButton = findViewById(R.id.startButton)
        startButtonShimmerRay = findViewById(R.id.startButtonShimmerRay)
        optionButton1 = findViewById(R.id.optionButton1)
        optionButton2 = findViewById(R.id.optionButton2)
        optionButton3 = findViewById(R.id.optionButton3)
        optionsContainer = findViewById(R.id.optionsContainer)
        resultCardContainer = findViewById(R.id.resultCardContainer)
        resultImageView = findViewById(R.id.resultImageView)
        resultExitButton = findViewById(R.id.resultExitButton)
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
        resultExitButton.visibility = View.GONE

        startButtonContainer.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE
        startButton.setImageResource(R.drawable.start_quiz)
        startButton.setOnClickListener { showWeatherQuestionPage() }

        startButtonContainer.post {
            startLoopingShimmer(startButtonShimmerRay, startButtonContainer.width)
        }
    }

    private fun showWeatherQuestionPage() {
        quizBackground.setImageResource(R.drawable.quiz1_background)

        stopShimmer()
        startButtonContainer.visibility = View.GONE
        startButton.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultCardContainer.visibility = View.GONE
        resultExitButton.visibility = View.GONE
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
        quizBackground.setImageResource(R.drawable.quiz2_background)

        stopShimmer()
        startButtonContainer.visibility = View.GONE
        startButton.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        resultCardContainer.visibility = View.GONE
        resultExitButton.visibility = View.GONE
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

        stopShimmer()
        startButtonContainer.visibility = View.GONE
        startButton.visibility = View.GONE
        optionsContainer.visibility = View.GONE
        resultCardContainer.visibility = View.GONE
        resultExitButton.visibility = View.GONE
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

        stopShimmer()
        loadingProgress.visibility = View.GONE
        startButtonContainer.visibility = View.GONE
        startButton.visibility = View.GONE
        optionsContainer.visibility = View.GONE
        resultCardContainer.visibility = View.VISIBLE
        resultImageView.setImageResource(resultDrawable)
        resultExitButton.visibility = View.VISIBLE
        resultExitButton.setOnClickListener { exitQuiz() }
    }

    private fun startLoopingShimmer(shimmerView: View, containerWidth: Int) {
        val startX = -shimmerView.width.toFloat() - 150f
        val endX = containerWidth.toFloat() + 150f

        shimmerAnimator?.cancel()
        shimmerAnimator = ObjectAnimator.ofFloat(shimmerView, "translationX", startX, endX).apply {
            duration = 1500
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    handler.postDelayed({
                        if (!isFinishing && startButtonContainer.visibility == View.VISIBLE) {
                            startLoopingShimmer(shimmerView, containerWidth)
                        }
                    }, 1500)
                }
            })
            start()
        }
    }

    private fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun exitQuiz() {
        when (intent.getStringExtra(EXTRA_ENTRY_SOURCE)) {
            ENTRY_LEVEL -> {
                val levelId = intent.getIntExtra(EXTRA_LEVEL_ID, -1)
                val targetIntent = Intent(this, LevelActivity::class.java)
                if (levelId != -1) {
                    targetIntent.putExtra("level", levelId)
                }
                startActivity(targetIntent)
            }

            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
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

        val maxPlan = when (weather) {
            Weather.SUNNY, Weather.RAIN -> 5
            Weather.CLOUD -> 4
        }
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
        stopShimmer()
    }
}
