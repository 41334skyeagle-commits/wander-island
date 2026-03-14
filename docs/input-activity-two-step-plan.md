# InputActivity 拆成兩頁的建議

## 結論（先回答你的問題）
建議用 **1 個 Activity + 2 個 Fragment + 共享 ViewModel（activity scope）**。

- 不建議繼續把兩頁都塞在同一個 `activity_input.xml` 用 `View.GONE/Visible` 切頁，因為後續維護會越來越難。
- 也不建議直接拆成 2 個 Activity，狀態同步（草稿、照片 Uri、返回流程）會比較麻煩。

## 為什麼這樣選
目前 `InputActivity` 同時處理：
- 首頁 UI（日期、標題、情緒、照片）
- 四題輸入 UI
- 相機/相簿權限與照片流程
- 最終組字串並寫入 DB

這些責任都集中在同一個 Activity，拆頁後若維持現況會讓程式繼續膨脹。

## 建議切頁方式
### Page 1（InputIntroFragment）
- 日期
- 標題
- 情緒
- 照片上傳
- 「下一步」按鈕

### Page 2（InputQuestionFragment）
- 四題內容輸入
- 「上一步」按鈕
- 「完成」按鈕（真正呼叫 `save`）

## 狀態管理（重點）
建立 `InputDraftViewModel`，以 `activityViewModels()` 讓兩個 Fragment 共享同一份草稿：

- `title: String`
- `emotion: enum`
- `photoUri: String?`
- `content1..content4: String`

切頁時不要直接寫 DB；只有按「完成」才組成內容並落庫。

## 導航建議
你可以二選一：

1. **Navigation Component（推薦）**
   - `InputActivity` 只放 `NavHostFragment`
   - 兩個 Fragment 用 action 前進/返回
   - 可擴充性最好

2. **ViewPager2（若你想滑動切頁）**
   - 禁用使用者左右滑（`isUserInputEnabled = false`），只允許按鈕切頁
   - 也可行，但表單流程通常 Navigation 直覺一些

## 漸進式重構順序（低風險）
1. 先抽 `InputDraftViewModel`（先不拆頁）
2. 把目前 `activity_input.xml` 切成 `fragment_input_intro.xml` + `fragment_input_question.xml`
3. `InputActivity` 改成單純容器（NavHost）
4. 把相機/相簿選圖邏輯放到 Page 1 Fragment（或抽成 helper）
5. `saveDiaryEntry()` 移到共用 use case / repository 呼叫點

## 你目前專案可直接沿用的重點
- 最終資料格式仍可沿用現在的四題合併字串規則
- 題目圖片命名與綁定邏輯可原封不動搬到 Page 2
- 日期顯示與情緒動畫可搬到 Page 1

## 什麼情況下不要用 Fragment？
如果你只是想「視覺上分段」但其實不需要返回、狀態保存、可擴充流程，才考慮同頁分區顯示。
但你目前已經有完整輸入流程與圖片上傳，**Fragment 會比較穩健**。
