package com.example.magicball_student

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random
import android.content.SharedPreferences

class GameView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs),
    SensorEventListener,
    Runnable {

    private lateinit var pref: SharedPreferences
    private var backgroundBitmap: Bitmap? = null

    private val BALL_COLOR = Color.RED
    private val TARGET_COLOR = Color.YELLOW
    private val BG_COLOR = Color.BLACK
    private val BALL_RADIUS = 45f
    private val TARGET_COUNT = 10
    private var SENSITIVITY = 0.3f

    private var ballX = -40f
    private var ballY = -20f
    private var ballSpeedX = 15f
    private var ballSpeedY = 15f
    private val targets = mutableListOf<Pair<Float, Float>>()
    private var score = 0
    private var bestScore = 0
    private var gameWon = false

    private val ballPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val targetPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var thread: Thread? = null
    private var isRunning = false

    init {

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg)

        for (i in 0 until TARGET_COUNT) {
            val x = Random.nextFloat() * (screenWidth - 200f) + 100f
            val y = Random.nextFloat() * (screenHeight - 200f) + 100f
            targets.add(Pair(x, y))
        }

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startGame()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopGame()
            }
        })
    }

    private fun startGame() {
        if (thread == null) {
            isRunning = true
            thread = Thread(this)
            thread?.start()
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopGame() {
        isRunning = false
        thread?.join()
        thread = null
        sensorManager.unregisterListener(this)
    }

    override fun run() {
        while (isRunning) {
            update()
            draw()
            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun update() {
        if (gameWon) return

        ballX += ballSpeedX
        ballY += ballSpeedY

        ballSpeedX *= 0.98f
        ballSpeedY *= 0.98f

        if (ballX - BALL_RADIUS < 0) {
            ballX = BALL_RADIUS
            ballSpeedX *= -0.5f
        }
        if (ballX + BALL_RADIUS > width) {
            ballX = width - BALL_RADIUS
            ballSpeedX *= -0.5f
        }
        if (ballY - BALL_RADIUS < 0) {
            ballY = BALL_RADIUS
            ballSpeedY *= -0.5f
        }
        if (ballY + BALL_RADIUS > height) {
            ballY = height - BALL_RADIUS
            ballSpeedY *= -0.5f
        }

        val toRemove = mutableListOf<Pair<Float, Float>>()
        for (target in targets) {
            val distance = Math.hypot(
                (ballX - target.first).toDouble(),
                (ballY - target.second).toDouble()
            )
            if (distance < BALL_RADIUS + 30f) {
                toRemove.add(target)
                score++

                SENSITIVITY *= 1.05f

                if (score > bestScore) {
                    bestScore = score
                }
            }
        }
        targets.removeAll(toRemove)
        if (targets.isEmpty()) {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            for (i in 0 until TARGET_COUNT) {
                val x = Random.nextFloat() * (screenWidth - 200f) + 100f
                val y = Random.nextFloat() * (screenHeight - 200f) + 100f
                targets.add(Pair(x, y))
            }
        }
    }

    private fun draw() {
        val holder = holder ?: return
        val canvas = holder.lockCanvas() ?: return

        try {
            if (backgroundBitmap != null) {
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(backgroundBitmap!!, width, height, true),
                    0f, 0f, null
                )
            } else {
                canvas.drawColor(BG_COLOR)
            }

            canvas.drawText("🐈", ballX, ballY + BALL_RADIUS, Paint().apply {
                textSize = BALL_RADIUS * 3f
                textAlign = Paint.Align.CENTER
            })

            for (target in targets) {
                canvas.drawText("\uD83E\uDD69", target.first, target.second + BALL_RADIUS, Paint().apply {
                    textSize = BALL_RADIUS * 1.5f
                    textAlign = Paint.Align.CENTER
                })
            }

            canvas.drawText("Счёт: $score", 350f, 100f, Paint().apply {
                color = Color.WHITE
                textSize = 40f
                setShadowLayer(8f, 4f, 4f, Color.BLACK)
            })

            canvas.drawText("Рекорд: $bestScore", 600f, 100f, Paint().apply {
                color = Color.WHITE
                textSize = 40f
                setShadowLayer(8f, 4f, 4f, Color.BLACK)
            })

            if (gameWon) {
                canvas.drawText("ПОБЕДА! 🎉", width / 2f, height / 2f, Paint().apply {
                    color = Color.YELLOW
                    textSize = 80f
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(10f, 5f, 5f, Color.BLACK)
                })
            }

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]

            ballSpeedX += x * SENSITIVITY
            ballSpeedY -= y * SENSITIVITY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}