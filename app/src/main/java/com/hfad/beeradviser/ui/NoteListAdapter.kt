package com.hfad.beeradviser.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.beeradviser.R
import com.hfad.beeradviser.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VIEW_TYPE_LARGE = 1
private const val VIEW_TYPE_SMALL = 2

interface OnItemClickListener {
    // 當列表項目被點擊時，回傳該筆日記的 ID
    fun onItemClick(noteId: Long)
}

class NoteListAdapter(
    private val notes: List<Note>,
    private val listener: OnItemClickListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class LargeNoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvYear: TextView = view.findViewById(R.id.tv_year_large)
        val tvMonth: TextView = view.findViewById(R.id.tv_month_large)
        val tvDay: TextView = view.findViewById(R.id.tv_day_large)
        val tvTitle: TextView = view.findViewById(R.id.tv_title_large)
    }

    class SmallNoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvYear: TextView = view.findViewById(R.id.tv_year_small)
        val tvMonthDay: TextView = view.findViewById(R.id.tv_month_day_small)
    }

    override fun getItemViewType(position: Int): Int {
        // 最上方 (position 0 到 4，共 5 筆) 使用大版縮圖
        return if (position < 5) {
            VIEW_TYPE_LARGE
        } else {
            VIEW_TYPE_SMALL
        }
    }

    override fun getItemCount(): Int {
        // 返回數據列表中項目的總數
        return notes.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View

        return when (viewType) {
            VIEW_TYPE_LARGE -> {
                // 載入大版縮圖的 XML 佈局
                view = inflater.inflate(R.layout.item_note_large, parent, false)
                // 創建並返回 LargeNoteViewHolder
                LargeNoteViewHolder(view)
            }

            VIEW_TYPE_SMALL -> {
                // 載入小版縮圖的 XML 佈局
                view = inflater.inflate(R.layout.item_note_small, parent, false)
                // 創建並返回 SmallNoteViewHolder
                SmallNoteViewHolder(view)
            }

            else -> {
                // 如果 viewType 錯誤，就拋出異常
                throw IllegalArgumentException("未知的視圖類型: $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = notes[position]
        Log.d("NoteAdapter", "Position: $position, Note Title: ${note.title}, ID: ${note.id}")

        // 1. 日期格式化邏輯
        val date = Date(note.timestamp)
        val yearFormat = SimpleDateFormat("yyyy", Locale.TAIWAN)
        val monthFormat = SimpleDateFormat("MM 月", Locale.TAIWAN)
        val dayFormat = SimpleDateFormat("dd 日", Locale.TAIWAN)
        val year = yearFormat.format(date)
        val month = monthFormat.format(date)
        val day = dayFormat.format(date)
        val monthAndDay = "$month $day"

        // 2. 判斷 ViewHolder 類型並綁定數據
        when (holder.itemViewType) {
            VIEW_TYPE_LARGE -> {
                // 這是大版縮圖的綁定邏輯
                val largeHolder = holder as LargeNoteViewHolder

                // 綁定 UI 元素
                largeHolder.tvYear.text = year
                largeHolder.tvMonth.text = month
                largeHolder.tvDay.text = day
                largeHolder.tvTitle.text = note.title // 綁定使用者輸入的標題/一句話
            }

            VIEW_TYPE_SMALL -> {
                // 這是小版縮圖的綁定邏輯
                val smallHolder = holder as SmallNoteViewHolder

                // 綁定 UI 元素
                smallHolder.tvYear.text = year
                smallHolder.tvMonthDay.text = monthAndDay
            }
        }
        holder.itemView.setOnClickListener {
            // 點擊時，呼叫介面方法，並傳遞日記 ID
            listener.onItemClick(note.id)
        }
    }
}