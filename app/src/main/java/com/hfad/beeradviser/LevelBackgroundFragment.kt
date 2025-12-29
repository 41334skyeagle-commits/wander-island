package com.hfad.beeradviser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class LevelBackgroundFragment : Fragment() {

    companion object {
        private const val ARG_BACKGROUND_RES_ID = "backgroundResId"

        fun newInstance(backgroundResId: Int): LevelBackgroundFragment {
            val fragment = LevelBackgroundFragment()
            val args = Bundle()
            args.putInt(ARG_BACKGROUND_RES_ID, backgroundResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_level_background, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backgroundImageView: ImageView = view.findViewById(R.id.backgroundImageView)
        val backgroundResId = arguments?.getInt(ARG_BACKGROUND_RES_ID) ?: R.drawable.default_level_background
        backgroundImageView.setImageResource(backgroundResId)
    }
}