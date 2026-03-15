package com.hfad.beeradviser.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.hfad.beeradviser.R
import com.hfad.beeradviser.SettingsFragment

class CopyrightFragment : Fragment(R.layout.fragment_copyright) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<ImageView>(R.id.copyrightCloseButton)
        closeButton.setOnClickListener {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }

        (activity as? SettingsFragment.SettingsChangeListener)?.onApplyBlurEffect(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? SettingsFragment.SettingsChangeListener)?.onApplyBlurEffect(false)
    }
}
