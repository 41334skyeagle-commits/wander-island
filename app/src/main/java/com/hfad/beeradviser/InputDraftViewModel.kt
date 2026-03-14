package com.hfad.beeradviser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class InputDraftViewModel(private val state: SavedStateHandle) : ViewModel() {

    enum class Emotion { NICE, OK, SAD }

    var currentPage: Int
        get() = state[KEY_CURRENT_PAGE] ?: PAGE_INTRO
        set(value) {
            state[KEY_CURRENT_PAGE] = value
        }

    var title: String
        get() = state[KEY_TITLE] ?: ""
        set(value) {
            state[KEY_TITLE] = value
        }

    var photoUri: String?
        get() = state[KEY_PHOTO_URI]
        set(value) {
            state[KEY_PHOTO_URI] = value
        }

    var emotion: Emotion?
        get() {
            val raw = state.get<String>(KEY_EMOTION) ?: return null
            return Emotion.entries.find { it.name == raw }
        }
        set(value) {
            state[KEY_EMOTION] = value?.name
        }

    var content1: String
        get() = state[KEY_CONTENT_1] ?: ""
        set(value) {
            state[KEY_CONTENT_1] = value
        }

    var content2: String
        get() = state[KEY_CONTENT_2] ?: ""
        set(value) {
            state[KEY_CONTENT_2] = value
        }

    var content3: String
        get() = state[KEY_CONTENT_3] ?: ""
        set(value) {
            state[KEY_CONTENT_3] = value
        }

    var content4: String
        get() = state[KEY_CONTENT_4] ?: ""
        set(value) {
            state[KEY_CONTENT_4] = value
        }

    fun canMoveToQuestions(): Boolean {
        return title.isNotBlank() || !photoUri.isNullOrBlank() || emotion != null
    }

    companion object {
        const val PAGE_INTRO = 1
        const val PAGE_QUESTIONS = 2

        private const val KEY_CURRENT_PAGE = "current_page"
        private const val KEY_TITLE = "title"
        private const val KEY_PHOTO_URI = "photo_uri"
        private const val KEY_EMOTION = "emotion"
        private const val KEY_CONTENT_1 = "content_1"
        private const val KEY_CONTENT_2 = "content_2"
        private const val KEY_CONTENT_3 = "content_3"
        private const val KEY_CONTENT_4 = "content_4"
    }
}
