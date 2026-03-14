package com.hfad.beeradviser

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import java.util.Date
import kotlin.math.abs

class InputQuestionFragment : Fragment() {

    interface Callback {
        fun onSwipeDownToIntroRequested()
    }

    companion object {
        private const val TAG = "InputQuestionFragment"
        private const val SWIPE_THRESHOLD_DP = 80
    }

    private val draftViewModel: InputDraftViewModel by activityViewModels()

    private lateinit var dateTextView: TextView
    private lateinit var downSwipeHintImageView: ImageView

    private var question1ImageView: ImageView? = null
    private var question2ImageView: ImageView? = null
    private var question3ImageView: ImageView? = null
    private var question4ImageView: ImageView? = null

    private lateinit var content1EditText: EditText
    private lateinit var content2EditText: EditText
    private lateinit var content3EditText: EditText
    private lateinit var content4EditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_input_questions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateTextView = view.findViewById(R.id.dateTextView)
        downSwipeHintImageView = view.findViewById(R.id.downSwipeHintImageView)

        question1ImageView = view.findViewById(R.id.question1TextView)
        question2ImageView = view.findViewById(R.id.question2TextView)
        question3ImageView = view.findViewById(R.id.question3TextView)
        question4ImageView = view.findViewById(R.id.question4ImageView)

        content1EditText = view.findViewById(R.id.content1EditText)
        content2EditText = view.findViewById(R.id.content2EditText)
        content3EditText = view.findViewById(R.id.content3EditText)
        content4EditText = view.findViewById(R.id.content4EditText)

        setupUI()
    }

    private fun setupUI() {
        val date = Date()
        dateTextView.text = DateFormat.format("yyyy年MM月dd日 EEEE", date)

        bindInputDecorations()
        bindDraft()

        setupSwipeHintTouch()
        startFloatingAnimation(downSwipeHintImageView)
    }

    private fun bindDraft() {
        content1EditText.setText(draftViewModel.content1)
        content2EditText.setText(draftViewModel.content2)
        content3EditText.setText(draftViewModel.content3)
        content4EditText.setText(draftViewModel.content4)

        content1EditText.doAfterTextChanged { draftViewModel.content1 = it }
        content2EditText.doAfterTextChanged { draftViewModel.content2 = it }
        content3EditText.doAfterTextChanged { draftViewModel.content3 = it }
        content4EditText.doAfterTextChanged { draftViewModel.content4 = it }
    }

    private fun EditText.doAfterTextChanged(block: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                block(s?.toString().orEmpty())
            }
        })
    }

    private fun setupSwipeHintTouch() {
        val thresholdPx = SWIPE_THRESHOLD_DP * resources.displayMetrics.density
        var startY = 0f

        downSwipeHintImageView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dy = event.rawY - startY
                    if (dy > 0 && abs(dy) >= thresholdPx) {
                        (activity as? Callback)?.onSwipeDownToIntroRequested()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun startFloatingAnimation(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, 12f, 0f).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun bindInputDecorations() {
        bindDrawableByName(question1ImageView, "inputq1")
        bindDrawableByName(question2ImageView, "inputq2")
        bindDrawableByName(question3ImageView, "inputq3")
        bindDrawableByName(question4ImageView, "inputq4")
    }

    private fun bindDrawableByName(target: ImageView?, drawableName: String) {
        target ?: return
        val resId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        if (resId != 0) {
            target.setImageResource(resId)
            target.visibility = View.VISIBLE
        } else {
            target.visibility = View.GONE
            Log.w(TAG, "找不到圖片資源：$drawableName")
        }
    }
}
