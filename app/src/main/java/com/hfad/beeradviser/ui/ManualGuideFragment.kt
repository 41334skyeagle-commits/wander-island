package com.hfad.beeradviser.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.hfad.beeradviser.R

class ManualGuideFragment : Fragment(R.layout.fragment_manual_guide) {

    private lateinit var manualPageImage: ImageView
    private val manualPages by lazy { loadManualPages() }
    private var currentPageIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manualPageImage = view.findViewById(R.id.manualPageImage)

        if (manualPages.isEmpty()) {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            return
        }

        renderCurrentPage()

        view.setOnClickListener {
            if (currentPageIndex < manualPages.lastIndex) {
                currentPageIndex += 1
                renderCurrentPage()
            } else {
                parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            }
        }
    }

    private fun renderCurrentPage() {
        manualPageImage.setImageResource(manualPages[currentPageIndex])
    }

    private fun loadManualPages(): List<Int> {
        val pages = mutableListOf<Int>()
        var pageNumber = 1

        while (true) {
            val resourceName = "manual_%02d".format(pageNumber)
            val resId = resources.getIdentifier(resourceName, "drawable", requireContext().packageName)
            if (resId == 0) break
            pages += resId
            pageNumber += 1
        }

        return pages
    }
}
