package com.example.digitalclock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

class EditStagesActivity : Activity() {

    private lateinit var layoutStages: LinearLayout
    private lateinit var btnBack: Button
    private lateinit var btnAddStage: Button
    private lateinit var btnSave: Button
    private lateinit var tvError: TextView
    private lateinit var sharedPrefs: SharedPreferences

    // Кнопки кастомной клавиатуры
    private lateinit var btn0: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btn5: Button
    private lateinit var btn6: Button
    private lateinit var btn7: Button
    private lateinit var btn8: Button
    private lateinit var btn9: Button
    private lateinit var btnColon: Button
    private lateinit var btnDelete: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button

    private val stagesList = mutableListOf<String>()
    private var selectedEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Предотвращаем выключение экрана
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_edit_stages)

        // Инициализация элементов
        layoutStages = findViewById(R.id.layoutStages)
        btnBack = findViewById(R.id.btnBack)
        btnAddStage = findViewById(R.id.btnAddStage)
        btnSave = findViewById(R.id.btnSave)
        tvError = findViewById(R.id.tvError)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)

        // Инициализация кнопок клавиатуры
        btn0 = findViewById(R.id.btn0)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        btn4 = findViewById(R.id.btn4)
        btn5 = findViewById(R.id.btn5)
        btn6 = findViewById(R.id.btn6)
        btn7 = findViewById(R.id.btn7)
        btn8 = findViewById(R.id.btn8)
        btn9 = findViewById(R.id.btn9)
        btnColon = findViewById(R.id.btnColon)
        btnDelete = findViewById(R.id.btnDelete)

        // SharedPreferences для сохранения данных
        sharedPrefs = getSharedPreferences("stages_data", Context.MODE_PRIVATE)

        // Загружаем сохраненные этапы
        loadStages()

        // Отображаем этапы
        displayStages()

        // Настраиваем кастомную клавиатуру
        setupCustomKeyboard()

        // Обработчики кнопок
        btnBack.setOnClickListener { finish() }
        btnAddStage.setOnClickListener {
            addNewStage()
        }
        btnSave.setOnClickListener {
            validateAndSaveCurrentField()
            saveStagesWithoutExit()
        }
        btnExport.setOnClickListener {
            exportStages()
        }

        btnImport.setOnClickListener {
            importStages()
        }
    }

    private fun loadStages() {
        stagesList.clear()
        val stagesCount = sharedPrefs.getInt("stages_count", 0)

        for (i in 0 until stagesCount) {
            val stageTime = sharedPrefs.getString("stage_$i", "") ?: ""
            if (stageTime.isNotEmpty()) {
                stagesList.add(stageTime)
            }
        }

        // Если нет этапов, добавляем один пустой для начала
        if (stagesList.isEmpty()) {
            stagesList.add("")
        }
    }

    private fun displayStages() {
        layoutStages.removeAllViews()

        stagesList.forEachIndexed { index, stageTime ->
            val stageView = createStageView(index + 1, stageTime)
            layoutStages.addView(stageView)
        }
    }

    private fun createStageView(stageNumber: Int, stageTime: String): View {
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        // Первая строка: номер, время, кнопка удаления
        val firstRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Номер этапа
        val tvNumber = TextView(this).apply {
            text = "$stageNumber)"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Поле ввода времени
        val etTime = EditText(this).apply {
            hint = "ЧЧ:ММ:СС"
            setText(stageTime)
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
            setBackgroundColor(0xFF333333.toInt())
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 8
            }

            // Отключаем стандартную клавиатуру
            showSoftInputOnFocus = false
            isFocusableInTouchMode = true

            // Обработчик получения фокуса
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    selectedEditText = this
                } else {
                    // При потере фокуса - валидируем и форматируем
                    validateAndFormatField(this)
                }
            }

            // Обработчик нажатия
            setOnClickListener {
                selectedEditText = this
                requestFocus()
            }
        }

        // Кнопка удаления
        val btnDelete = Button(this).apply {
            text = "Удалить"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFCC0000.toInt())
            layoutParams = LinearLayout.LayoutParams(100, 50)
            setOnClickListener {
                deleteStage(stageNumber - 1)
            }
        }

        firstRow.addView(tvNumber)
        firstRow.addView(etTime)
        firstRow.addView(btnDelete)

        // Вторая строка: примечание
        val etNote = EditText(this).apply {
            hint = "Примечание (озеро, поворот, мост...)"
            val savedNote = sharedPrefs.getString("stage_note_${stageNumber - 1}", "") ?: ""
            setText(savedNote)
            textSize = 14f
            setTextColor(0xFFFFCC00.toInt())
            setHintTextColor(0xFF666666.toInt())
            setBackgroundColor(0xFF222222.toInt())
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
                leftMargin = 60 // Выравниваем с полем времени
            }
            maxLines = 1
            setSingleLine(true)
        }

        linearLayout.addView(firstRow)
        linearLayout.addView(etNote)

        return linearLayout
    }

    private fun addNewStage() {
        // Сначала сохраняем все текущие значения в список
        updateStagesListFromFields()

        // Добавляем новый пустой этап
        stagesList.add("")

        // Обновляем отображение
        displayStages()
    }

    private fun updateStagesListFromFields() {
        stagesList.clear()

        for (i in 0 until layoutStages.childCount) {
            val stageView = layoutStages.getChildAt(i) as LinearLayout
            val firstRow = stageView.getChildAt(0) as LinearLayout
            val etTime = firstRow.getChildAt(1) as EditText
            val etNote = stageView.getChildAt(1) as EditText

            val timeText = etTime.text.toString().trim()
            val noteText = etNote.text.toString().trim()

            // Валидируем и форматируем каждое поле
            if (timeText.isNotEmpty()) {
                val formatted = autoFormatTime(timeText)
                if (isValidTimeInput(formatted)) {
                    etTime.setText(formatted)
                    etTime.setTextColor(0xFFFFFFFF.toInt())
                    stagesList.add(formatted)

                    // Сохраняем примечание
                    sharedPrefs.edit().putString("stage_note_$i", noteText).apply()
                } else {
                    etTime.setTextColor(0xFFFF6666.toInt())
                    stagesList.add(timeText) // Добавляем как есть, чтобы не потерять
                }
            } else {
                stagesList.add("")
                // Сохраняем примечание даже если время пустое
                sharedPrefs.edit().putString("stage_note_$i", noteText).apply()
            }
        }
    }

    private fun deleteStage(index: Int) {
        if (stagesList.size > 1) { // Оставляем хотя бы один этап
            stagesList.removeAt(index)
            displayStages()
        }
    }

    private fun saveStagesWithoutExit() {
        hideError()

        // Валидируем все поля перед сохранением
        val newStagesList = mutableListOf<String>()
        var hasError = false

        for (i in 0 until layoutStages.childCount) {
            val stageView = layoutStages.getChildAt(i) as LinearLayout
            val firstRow = stageView.getChildAt(0) as LinearLayout
            val etTime = firstRow.getChildAt(1) as EditText
            val etNote = stageView.getChildAt(1) as EditText

            val timeText = etTime.text.toString().trim()
            val noteText = etNote.text.toString().trim()

            if (timeText.isNotEmpty()) {
                val formatted = autoFormatTime(timeText)
                if (isValidTimeInput(formatted)) {
                    etTime.setText(formatted)
                    etTime.setTextColor(0xFFFFFFFF.toInt())
                    newStagesList.add(formatted)

                    // Сохраняем примечание
                    sharedPrefs.edit().putString("stage_note_$i", noteText).apply()
                } else {
                    etTime.setTextColor(0xFFFF6666.toInt())
                    showError("Неверный формат времени в этапе ${i + 1}")
                    hasError = true
                }
            } else {
                // Сохраняем примечание даже если время пустое
                sharedPrefs.edit().putString("stage_note_$i", noteText).apply()
            }
        }

        if (hasError) return

        // Проверяем, что времена идут по возрастанию
        if (!isTimesInOrder(newStagesList)) {
            showError("Время каждого следующего этапа должно быть больше предыдущего")
            return
        }

        // Сохраняем в SharedPreferences
        val editor = sharedPrefs.edit()
        editor.putInt("stages_count", newStagesList.size)

        newStagesList.forEachIndexed { index, time ->
            editor.putString("stage_$index", time)
        }

        editor.apply()

        Toast.makeText(this, "Этапы сохранены", Toast.LENGTH_SHORT).show()
        // НЕ вызываем finish() - остаемся на экране редактирования
    }

    private fun saveStages() {
        saveStagesWithoutExit()
        finish() // Только эта функция закрывает экран
    }

    private fun isValidTimeFormat(time: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            format.isLenient = false
            format.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isTimesInOrder(times: List<String>): Boolean {
        if (times.size <= 1) return true

        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (i in 1 until times.size) {
            val prevTime = format.parse(times[i - 1])
            val currTime = format.parse(times[i])

            if (currTime <= prevTime) {
                return false
            }
        }

        return true
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }

    private fun setupCustomKeyboard() {
        // Обработчики цифровых кнопок
        btn0.setOnClickListener { insertText("0") }
        btn1.setOnClickListener { insertText("1") }
        btn2.setOnClickListener { insertText("2") }
        btn3.setOnClickListener { insertText("3") }
        btn4.setOnClickListener { insertText("4") }
        btn5.setOnClickListener { insertText("5") }
        btn6.setOnClickListener { insertText("6") }
        btn7.setOnClickListener { insertText("7") }
        btn8.setOnClickListener { insertText("8") }
        btn9.setOnClickListener { insertText("9") }
        btnColon.setOnClickListener { insertText(":") }
        btnDelete.setOnClickListener { deleteLastChar() }
    }

    private fun insertText(text: String) {
        selectedEditText?.let { editText ->
            val currentText = editText.text.toString()
            val cursorPosition = editText.selectionStart

            val newText = currentText.substring(0, cursorPosition) + text + currentText.substring(cursorPosition)

            // Проверяем ограничения при вводе
            if (isValidInput(newText)) {
                editText.setText(newText)
                editText.setSelection(cursorPosition + text.length)
            }
        }
    }

    private fun isValidInput(input: String): Boolean {
        // Убираем лишние символы
        val cleaned = input.replace(Regex("[^0-9:]"), "")

        // Не больше 8 символов (ЧЧ:ММ:СС)
        if (cleaned.length > 8) return false

        // Не больше 2 двоеточий
        if (cleaned.count { it == ':' } > 2) return false

        // Проверяем части между двоеточиями
        val parts = cleaned.split(":")
        return parts.all { part ->
            // Каждая часть не больше 2 цифр
            part.length <= 2 && (part.isEmpty() || part.all { it.isDigit() })
        }
    }

    private fun deleteLastChar() {
        selectedEditText?.let { editText ->
            val currentText = editText.text.toString()
            val cursorPosition = editText.selectionStart

            if (cursorPosition > 0) {
                val newText = currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition)
                editText.setText(newText)
                editText.setSelection(cursorPosition - 1)
            }
        }
    }

    private fun validateAndSaveCurrentField() {
        selectedEditText?.let { editText ->
            validateAndFormatField(editText)
        }
    }

    private fun validateAndFormatField(editText: EditText) {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) return

        // Автоформатирование - добавляем двоеточия если забыли
        val formatted = autoFormatTime(text)

        // Проверяем корректность
        if (isValidTimeInput(formatted)) {
            editText.setText(formatted)
            editText.setTextColor(0xFFFFFFFF.toInt()) // Белый - корректное время
            hideError()
        } else {
            editText.setTextColor(0xFFFF6666.toInt()) // Красноватый - ошибка
            showError("Некорректное время. Формат: ЧЧ:ММ:СС (часы 00-23, минуты/секунды 00-59)")
        }
    }

    private fun autoFormatTime(input: String): String {
        // Убираем все лишние символы, оставляем только цифры и двоеточия
        val cleaned = input.replace(Regex("[^0-9:]"), "")

        // Если только цифры - добавляем двоеточия
        if (!cleaned.contains(":")) {
            return when (cleaned.length) {
                1 -> "0$cleaned:00:00"
                2 -> "$cleaned:00:00"
                3 -> "${cleaned.substring(0, 1)}${cleaned.substring(1, 2)}:${cleaned.substring(2, 3)}0:00"
                4 -> "${cleaned.substring(0, 2)}:${cleaned.substring(2, 4)}:00"
                5 -> "${cleaned.substring(0, 2)}:${cleaned.substring(2, 4)}:0${cleaned.substring(4, 5)}"
                6 -> "${cleaned.substring(0, 2)}:${cleaned.substring(2, 4)}:${cleaned.substring(4, 6)}"
                else -> cleaned
            }
        }

        return cleaned
    }

    private fun isValidTimeInput(time: String): Boolean {
        if (time.isEmpty()) return true

        // Проверяем формат ЧЧ:ММ:СС
        val parts = time.split(":")
        if (parts.size != 3) return false

        return try {
            // Проверяем что каждая часть состоит из 1-2 цифр
            if (parts.any { it.length == 0 || it.length > 2 }) return false

            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val seconds = parts[2].toInt()

            // Проверяем диапазоны
            hours in 0..23 && minutes in 0..59 && seconds in 0..59
        } catch (e: NumberFormatException) {
            false
        }
    }
    private fun exportStages() {
        try {
            // Собираем данные этапов
            val exportData = StringBuilder()

            for (i in 0 until layoutStages.childCount) {
                val stageView = layoutStages.getChildAt(i) as LinearLayout
                val firstRow = stageView.getChildAt(0) as LinearLayout
                val etTime = firstRow.getChildAt(1) as EditText
                val etNote = stageView.getChildAt(1) as EditText

                val timeText = etTime.text.toString().trim()
                val noteText = etNote.text.toString().trim()

                if (timeText.isNotEmpty()) {
                    exportData.appendLine("$timeText|$noteText")
                }
            }

            // Создаем имя файла по умолчанию
            val currentDate = java.text.SimpleDateFormat("yyyy_MM_dd", java.util.Locale.getDefault()).format(java.util.Date())
            val defaultFileName = "rally_stages_$currentDate.txt"

            // Показываем диалог ввода имени файла
            showFileNameDialog(exportData.toString(), defaultFileName)

        } catch (e: Exception) {
            showError("Ошибка экспорта: ${e.message}")
        }
    }

    private fun showFileNameDialog(content: String, defaultFileName: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Экспорт этапов")

        // Создаем поле ввода имени файла
        val input = EditText(this)
        input.setText(defaultFileName)
        input.setTextColor(0xFFFFFFFF.toInt())
        input.setBackgroundColor(0xFF333333.toInt())
        input.setPadding(20, 20, 20, 20)

        builder.setView(input)
        builder.setMessage("Введите имя файла:")

        builder.setPositiveButton("Сохранить") { dialog, _ ->
            val fileName = input.text.toString().trim()
            if (fileName.isNotEmpty()) {
                saveFileToDownloads(content, fileName)
            } else {
                showError("Имя файла не может быть пустым")
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun saveFileToDownloads(content: String, fileName: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ - используем MediaStore
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    Toast.makeText(this, "Экспорт прошел успешно\nФайл сохранен в Downloads: $fileName", Toast.LENGTH_LONG).show()
                } ?: run {
                    showError("Не удалось создать файл")
                }

            } else {
                // Android 9 и ниже - старый способ
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(content)

                Toast.makeText(this, "Экспорт прошел успешно\nФайл сохранен: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            showError("Ошибка сохранения файла: ${e.message}")
        }
    }

    private fun importStages() {
        try {
            // Создаем Intent для выбора файла
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "text/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            startActivityForResult(Intent.createChooser(intent, "Выберите файл с этапами"), 1001)

        } catch (e: Exception) {
            showError("Ошибка открытия файла: ${e.message}")
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val content = inputStream?.bufferedReader()?.use { it.readText() }

                    if (content != null) {
                        parseAndImportStages(content)
                    } else {
                        showError("Не удалось прочитать файл")
                    }

                } catch (e: Exception) {
                    showError("Ошибка чтения файла: ${e.message}")
                }
            }
        }
    }

    private fun parseAndImportStages(content: String) {
        try {
            val lines = content.trim().split("\n")
            val newStages = mutableListOf<Pair<String, String>>()

            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                val parts = line.split("|")
                if (parts.size >= 1) {
                    val time = parts[0].trim()
                    val note = if (parts.size > 1) parts[1].trim() else ""

                    // Проверяем формат времени
                    if (isValidTimeFormat(time)) {
                        newStages.add(Pair(time, note))
                    } else {
                        showError("Ошибка в строке ${i + 1}: некорректное время '$time'")
                        return
                    }
                } else {
                    showError("Ошибка в строке ${i + 1}: неверный формат")
                    return
                }
            }

            // Проверяем порядок времени
            if (!isTimesInOrder(newStages.map { it.first })) {
                showError("Ошибка: времена этапов должны идти по возрастанию")
                return
            }

            // Импортируем этапы
            importValidatedStages(newStages)

        } catch (e: Exception) {
            showError("Ошибка парсинга файла: ${e.message}")
        }
    }

    private fun importValidatedStages(stages: List<Pair<String, String>>) {
        // Очищаем текущие этапы
        stagesList.clear()
        stages.forEach { stage ->
            stagesList.add(stage.first)
            // Сохраняем примечания
            val index = stagesList.size - 1
            sharedPrefs.edit().putString("stage_note_$index", stage.second).apply()
        }

        // Обновляем отображение
        displayStages()

        Toast.makeText(this, "Импорт прошел успешно\nЗагружено ${stages.size} этапов", Toast.LENGTH_LONG).show()
    }

}