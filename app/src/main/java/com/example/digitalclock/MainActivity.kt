package com.example.digitalclock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.View

class MainActivity : Activity() {

    private lateinit var tvHour1: TextView
    private lateinit var tvHour2: TextView
    private lateinit var tvMin1: TextView
    private lateinit var tvMin2: TextView
    private lateinit var tvSec1: TextView
    private lateinit var tvSec2: TextView
    private lateinit var tvColon1: TextView
    private lateinit var tvColon2: TextView
    private lateinit var tvStageTime: TextView
    private lateinit var tvStageNote: TextView
    private lateinit var tvTimeDiff: TextView
    private lateinit var btnMirror: Button
    private lateinit var btnEditStages: Button
    private lateinit var btnFontIncrease: Button
    private lateinit var btnFontDecrease: Button
    private lateinit var btnBrightnessIncrease: Button
    private lateinit var btnBrightnessDecrease: Button
    private lateinit var layoutClock: LinearLayout
    private lateinit var timeLayout: LinearLayout
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var sharedPrefs: SharedPreferences
    private var isMirrored = false

    private lateinit var digitViews: List<TextView>
    private lateinit var allHudViews: List<TextView>
    private var currentStageTime: String? = null
    private var currentFontScale = 1.0f
    private var currentBrightness = 1.0f
    // Секундомеры
    private lateinit var tvStopwatch1: TextView
    private lateinit var tvStopwatch2: TextView
    private var stopwatch1Time = 0L // время в миллисекундах
    private var stopwatch2Time = 0L
    private var stopwatch1Running = false
    private var stopwatch2Running = false
    private var stopwatch1StartTime = 0L
    private var stopwatch2StartTime = 0L
    private var stopwatch1WasRunning = false
    private var stopwatch2WasRunning = false

    // Для определения двойного клика
    private var lastClick1Time = 0L
    private var lastClick2Time = 0L
    private val DOUBLE_CLICK_DELAY = 500L // 0.5 секунды

    // Базовые размеры шрифтов
    private val baseFontSizes = mapOf(
        "main_time" to 150f,    // Основное время
        "stage_time" to 80f,    // Время этапа (было 100f)
        "diff_time" to 100f     // Разность времени (было 80f)
    )

    private fun updateFontSizes(scale: Float) {
        // Основное время (цифры и двоеточия)
        val mainTimeSize = baseFontSizes["main_time"]!! * scale
        digitViews.forEach { it.textSize = mainTimeSize }
        listOf(tvColon1, tvColon2).forEach { it.textSize = mainTimeSize }

        // Время этапа
        val stageTimeSize = baseFontSizes["stage_time"]!! * scale
        tvStageTime.textSize = stageTimeSize

        // Примечание к этапу (пропорционально уменьшается)
        val noteSize = 48f * scale
        tvStageNote.textSize = noteSize

        // Разность времени
        val diffTimeSize = baseFontSizes["diff_time"]!! * scale
        tvTimeDiff.textSize = diffTimeSize


        // Секундомеры (базовый размер 70sp)
        val stopwatchSize = 70f * scale
        tvStopwatch1.textSize = stopwatchSize
        tvStopwatch2.textSize = stopwatchSize
    }

    private fun updateBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    private fun applyHudMode() {
        val scaleValue = if (isMirrored) -1f else 1f

        // Отзеркаливаем все элементы только по вертикали (scaleY)
        allHudViews.forEach { textView ->
            textView.scaleY = scaleValue
            textView.scaleX = 1f  // Горизонтальное отзеркаливание всегда отключено
        }

        // Также отзеркаливаем кнопки управления
        btnFontIncrease.scaleY = scaleValue
        btnFontDecrease.scaleY = scaleValue
        btnFontIncrease.scaleX = 1f
        btnFontDecrease.scaleX = 1f

        btnBrightnessIncrease.scaleY = scaleValue
        btnBrightnessDecrease.scaleY = scaleValue
        btnBrightnessIncrease.scaleX = 1f
        btnBrightnessDecrease.scaleX = 1f

        // Изменяем порядок элементов в HUD режиме
        layoutClock.removeAllViews()

        if (isMirrored) {
            // HUD режим: время этапа, примечание, разность, текущее время (снизу вверх)
            layoutClock.addView(tvStageTime)
            layoutClock.addView(tvStageNote)
            layoutClock.addView(tvTimeDiff)
            layoutClock.addView(timeLayout)
        } else {
            // Обычный режим: текущее время, разность, примечание, время этапа (сверху вниз)
            layoutClock.addView(timeLayout)
            layoutClock.addView(tvTimeDiff)
            layoutClock.addView(tvStageNote)
            layoutClock.addView(tvStageTime)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Полноэкранный режим
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // Предотвращаем выключение экрана и затемнение
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

        // SharedPreferences для загрузки настроек
        sharedPrefs = getSharedPreferences("stages_data", Context.MODE_PRIVATE)

        // Устанавливаем сохраненную яркость
        currentBrightness = sharedPrefs.getFloat("brightness", 1.0f)
        updateBrightness(currentBrightness)

        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        tvHour1 = findViewById(R.id.tvHour1)
        tvHour2 = findViewById(R.id.tvHour2)
        tvMin1 = findViewById(R.id.tvMin1)
        tvMin2 = findViewById(R.id.tvMin2)
        tvSec1 = findViewById(R.id.tvSec1)
        tvSec2 = findViewById(R.id.tvSec2)
        tvColon1 = findViewById(R.id.tvColon1)
        tvColon2 = findViewById(R.id.tvColon2)
        tvStageTime = findViewById(R.id.tvStageTime)
        tvStageNote = findViewById(R.id.tvStageNote)
        tvTimeDiff = findViewById(R.id.tvTimeDiff)
        btnMirror = findViewById(R.id.btnMirror)
        tvStopwatch1 = findViewById(R.id.tvStopwatch1)
        tvStopwatch2 = findViewById(R.id.tvStopwatch2)
        btnEditStages = findViewById(R.id.btnEditStages)
        btnFontIncrease = findViewById(R.id.btnFontIncrease)
        btnFontDecrease = findViewById(R.id.btnFontDecrease)
        btnBrightnessIncrease = findViewById(R.id.btnBrightnessIncrease)
        btnBrightnessDecrease = findViewById(R.id.btnBrightnessDecrease)
        layoutClock = findViewById(R.id.layoutClock)

        // Находим layout с цифрами времени
        timeLayout = layoutClock.getChildAt(0) as LinearLayout

        // Список всех цифровых TextView (без двоеточий)
        digitViews = listOf(tvHour1, tvHour2, tvMin1, tvMin2, tvSec1, tvSec2)

        // Все элементы для HUD режима
        allHudViews = listOf(tvHour1, tvHour2, tvMin1, tvMin2, tvSec1, tvSec2,
            tvColon1, tvColon2, tvStageTime, tvStageNote, tvTimeDiff,
            tvStopwatch1, tvStopwatch2)  // ДОБАВИТЬ секундомеры

        // Загружаем сохраненный размер шрифта
        currentFontScale = sharedPrefs.getFloat("font_scale", 1.0f)
        updateFontSizes(currentFontScale)

        // Настройка кнопок изменения размера шрифта
        btnFontIncrease.setOnClickListener {
            if (currentFontScale < 1.0f) {
                currentFontScale += 0.1f
                if (currentFontScale > 1.0f) currentFontScale = 1.0f
                updateFontSizes(currentFontScale)
                sharedPrefs.edit().putFloat("font_scale", currentFontScale).apply()
            }
        }

        btnFontDecrease.setOnClickListener {
            if (currentFontScale > 0.5f) {
                currentFontScale -= 0.1f
                if (currentFontScale < 0.5f) currentFontScale = 0.5f
                updateFontSizes(currentFontScale)
                sharedPrefs.edit().putFloat("font_scale", currentFontScale).apply()
            }
        }

        // Настройка кнопок изменения яркости
        btnBrightnessIncrease.setOnClickListener {
            if (currentBrightness < 1.0f) {
                currentBrightness += 0.05f // Уменьшили шаг до 5%
                if (currentBrightness > 1.0f) currentBrightness = 1.0f
                updateBrightness(currentBrightness)
                sharedPrefs.edit().putFloat("brightness", currentBrightness).apply()
            }
        }

        btnBrightnessDecrease.setOnClickListener {
            if (currentBrightness > 0.001f) {
                currentBrightness -= 0.05f // Уменьшили шаг до 5%
                if (currentBrightness < 0.001f) currentBrightness = 0.001f // Минимум 0.1%
                updateBrightness(currentBrightness)
                sharedPrefs.edit().putFloat("brightness", currentBrightness).apply()
            }
        }

        // Обработчики кнопок
        btnMirror.setOnClickListener { toggleMirror() }
        btnEditStages.setOnClickListener {
            startActivity(Intent(this, EditStagesActivity::class.java))
        }
        // Обработчики секундомеров
        tvStopwatch1.setOnClickListener {
            handleStopwatchClick(1)
        }

        tvStopwatch2.setOnClickListener {
            handleStopwatchClick(2)
        }



        // Инициализация Handler для обновления времени
        handler = Handler(Looper.getMainLooper())

        // Создаем Runnable для обновления времени
        runnable = object : Runnable {
            override fun run() {
                updateTime()
                updateStopwatches() // Дополнительное обновление секундомеров
                handler.postDelayed(this, 50) // Обновляем каждые 50мс (20 раз в секунду)
            }
        }

        // Запускаем обновление времени
        handler.post(runnable)
    }

    private fun toggleMirror() {
        isMirrored = !isMirrored
        applyHudMode()

        // Обновляем текст кнопки
        btnMirror.text = if (isMirrored) "NORMAL" else "HUD"
    }

    override fun onResume() {
        super.onResume()
        // Обновляем информацию об этапах при возвращении с экрана редактирования
        handler.post {
            val calendar = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentTime = timeFormat.format(calendar.time)
            updateStageInfo(currentTime)
        }
    }

    private fun updateTime() {
        val calendar = Calendar.getInstance()

        // Формат времени: чч:мм:сс
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(calendar.time)

        // Разбиваем время на отдельные символы
        val timeChars = currentTime.toCharArray()
        tvHour1.text = timeChars[0].toString()
        tvHour2.text = timeChars[1].toString()
        tvMin1.text = timeChars[3].toString()
        tvMin2.text = timeChars[4].toString()
        tvSec1.text = timeChars[6].toString()
        tvSec2.text = timeChars[7].toString()

        // Обновляем информацию об этапах
        updateStageInfo(currentTime)
        // ДОБАВИТЬ ЭТУ СТРОКУ:
        //updateStopwatches()
    }

    private fun updateStageInfo(currentTime: String) {
        val nextStageData = findNextStageData(currentTime)

        if (nextStageData != null) {
            tvStageTime.text = nextStageData.first
            tvStageTime.visibility = View.VISIBLE

            // Показываем примечание только если оно не пустое
            val noteText = nextStageData.second
            if (noteText.isNotEmpty()) {
                tvStageNote.text = noteText
                tvStageNote.visibility = View.VISIBLE
            } else {
                tvStageNote.text = ""
                tvStageNote.visibility = View.GONE
            }

            tvTimeDiff.visibility = View.VISIBLE
            updateTimeDifference(currentTime, nextStageData.first)
            currentStageTime = nextStageData.first
        } else {
            tvStageTime.text = ""
            tvStageTime.visibility = View.GONE
            tvStageNote.text = ""
            tvStageNote.visibility = View.GONE
            tvTimeDiff.text = ""
            tvTimeDiff.visibility = View.GONE
            currentStageTime = null
        }

        // Применяем текущее состояние HUD режима
        applyHudMode()
    }

    private fun findNextStageData(currentTime: String): Pair<String, String>? {
        val stagesCount = sharedPrefs.getInt("stages_count", 0)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        try {
            val currentTimeObj = timeFormat.parse(currentTime)

            for (i in 0 until stagesCount) {
                val stageTime = sharedPrefs.getString("stage_$i", "") ?: ""
                val stageNote = sharedPrefs.getString("stage_note_$i", "") ?: ""

                if (stageTime.isNotEmpty()) {
                    val stageTimeObj = timeFormat.parse(stageTime)

                    // Если время этапа больше текущего времени
                    if (stageTimeObj > currentTimeObj) {
                        return Pair(stageTime, stageNote)
                    }
                }
            }
        } catch (e: Exception) {
            // Ошибка парсинга времени
        }

        return null // Нет следующих этапов
    }

    private fun updateTimeDifference(currentTime: String, stageTime: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        try {
            val currentTimeObj = timeFormat.parse(currentTime)
            val stageTimeObj = timeFormat.parse(stageTime)

            val diffMs = stageTimeObj.time - currentTimeObj.time
            val diffSeconds = diffMs / 1000

            if (diffSeconds > 0) {
                val minutes = diffSeconds / 60
                val seconds = diffSeconds % 60

                val diffText = String.format("%02d:%02d", minutes, seconds)
                tvTimeDiff.text = diffText

                // Меняем цвет на желтый за 10 секунд до нуля
                if (diffSeconds <= 10) {
                    tvTimeDiff.setTextColor(Color.YELLOW)
                } else {
                    tvTimeDiff.setTextColor(Color.WHITE)
                }
            } else {
                tvTimeDiff.text = "00:00"
                tvTimeDiff.setTextColor(Color.WHITE)
            }

        } catch (e: Exception) {
            tvTimeDiff.text = "--:--"
            tvTimeDiff.setTextColor(Color.WHITE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Останавливаем обновление времени при закрытии приложения
        handler.removeCallbacks(runnable)
    }

    private fun handleStopwatchClick(stopwatchNumber: Int) {
        val currentTime = System.currentTimeMillis()

        if (stopwatchNumber == 1) {
            if (currentTime - lastClick1Time <= DOUBLE_CLICK_DELAY) {
                // Двойной клик - сброс
                resetStopwatch(1)
                lastClick1Time = 0L
            } else {
                // Одиночный клик - мгновенное действие
                toggleStopwatchImmediate(1, currentTime)
                lastClick1Time = currentTime
            }
        } else {
            if (currentTime - lastClick2Time <= DOUBLE_CLICK_DELAY) {
                // Двойной клик - сброс
                resetStopwatch(2)
                lastClick2Time = 0L
            } else {
                // Одиночный клик - мгновенное действие
                toggleStopwatchImmediate(2, currentTime)
                lastClick2Time = currentTime
            }
        }
    }
    private fun toggleStopwatchImmediate(stopwatchNumber: Int, currentTime: Long) {
        if (stopwatchNumber == 1) {
            if (stopwatch1Running) {
                // Пауза - сохраняем накопленное время
                stopwatch1Time += currentTime - stopwatch1StartTime
                stopwatch1Running = false
                // Мгновенно показываем результат
                tvStopwatch1.text = formatStopwatchTime(stopwatch1Time)
            } else {
                // Старт
                stopwatch1StartTime = currentTime
                stopwatch1Running = true
                // Если время уже накоплено, показываем его, иначе 00:00
                tvStopwatch1.text = formatStopwatchTime(stopwatch1Time)
            }
        } else {
            if (stopwatch2Running) {
                // Пауза - сохраняем накопленное время
                stopwatch2Time += currentTime - stopwatch2StartTime
                stopwatch2Running = false
                // Мгновенно показываем результат
                tvStopwatch2.text = formatStopwatchTime(stopwatch2Time)
            } else {
                // Старт
                stopwatch2StartTime = currentTime
                stopwatch2Running = true
                // Если время уже накоплено, показываем его, иначе 00:00
                tvStopwatch2.text = formatStopwatchTime(stopwatch2Time)
            }
        }
    }



    private fun resetStopwatch(stopwatchNumber: Int) {
        if (stopwatchNumber == 1) {
            // Возвращаем к состоянию до последнего клика, затем сбрасываем
            stopwatch1Time = 0L
            stopwatch1Running = false
            stopwatch1StartTime = 0L
            tvStopwatch1.text = "00:00"
        } else {
            stopwatch2Time = 0L
            stopwatch2Running = false
            stopwatch2StartTime = 0L
            tvStopwatch2.text = "00:00"
        }
    }

    private fun updateStopwatches() {
        val currentTime = System.currentTimeMillis()

        // Обновляем секундомер 1
        if (stopwatch1Running) {
            val totalTime = stopwatch1Time + (currentTime - stopwatch1StartTime)
            tvStopwatch1.text = formatStopwatchTime(totalTime)
        }

        // Обновляем секундомер 2
        if (stopwatch2Running) {
            val totalTime = stopwatch2Time + (currentTime - stopwatch2StartTime)
            tvStopwatch2.text = formatStopwatchTime(totalTime)
        }
    }

    private fun formatStopwatchTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}