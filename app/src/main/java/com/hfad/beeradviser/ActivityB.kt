package com.hfad.beeradviser

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hfad.beeradviser.data.NoteDatabaseHelper
import com.hfad.beeradviser.ui.NoteListAdapter
import com.hfad.beeradviser.ui.OnItemClickListener

class ActivityB : AppCompatActivity(), OnItemClickListener {

    private lateinit var imageButtonCreateNote: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var dbHelper: NoteDatabaseHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_b)

        imageButtonCreateNote = findViewById(R.id.imageButton_create_note)
        recyclerViewNotes = findViewById(R.id.recyclerView_notes)
        backButton = findViewById(R.id.backButton)
        dbHelper = NoteDatabaseHelper(this)

        backButton.setOnClickListener {
            // 呼叫 finish() 即可返回上一個 Activity
            finish()
        }

        imageButtonCreateNote.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()

        loadNotes()
    }

    private fun loadNotes() {
        // 從資料庫獲取所有筆記
        val notes = dbHelper.allNotes

        // 創建並設置 Adapter
        val adapter = NoteListAdapter(notes, this)
        recyclerViewNotes.adapter = adapter
    }
    override fun onItemClick(noteId: Long) {
        // 1. 創建 Intent 導航到 NoteDetailActivity
        val intent = Intent(this, NoteDetailActivity::class.java)

        // 2. 將被點擊日記的 ID 傳遞給 DetailActivity
        intent.putExtra("NOTE_ID", noteId)

        // 3. 啟動 Activity
        startActivity(intent)
    }
}