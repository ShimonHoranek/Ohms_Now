/*
 * GNU General Public License v3.0
 *
 * Copyright (c) 2021 Toh Jeen Gie Keith
 *
 *
 * This file is part of Ohms Now!.
 *
 * Ohms Now! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ohms Now! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ohms Now!.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ktprograms.ohmsnow

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    // View references
    private lateinit var screen: ConstraintLayout
    private lateinit var resistorBody: ImageView
    private lateinit var band1: ImageButton
    private lateinit var band2: ImageButton
    private lateinit var bandMultiplier: ImageButton
    private lateinit var bandLast: ImageButton
    private lateinit var ohmsTextView: TextView

    // Band color states
    private var band1State = Band(BandColors.BLUE)
    private var band2State = Band(BandColors.GREY)
    private var bandMultiplierState = MultiplierBand(MultiplierBandColors.RED)
    private var bandLastState = ToleranceBandColors.GOLD

    // Touched band
    private var touchedBand: Int = -1

    // Had no long press
    private var hadNoLongPress = true

    // X coordinate on ACTION_DOWN
    private var previousX = 0F

    // Minimum swipe amount
    private val MIN_DISTANCE = 100

    // E12 / E24 values
    private val e12 = listOf(
        Pair(1, 0),
        Pair(1, 2),
        Pair(1, 5),
        Pair(1, 8),
        Pair(2, 2),
        Pair(2, 7),
        Pair(3, 3),
        Pair(3, 9),
        Pair(4, 7),
        Pair(5, 6),
        Pair(6, 8),
        Pair(8, 2)
    )
    private val e24 = listOf(
        Pair(1, 0),
        Pair(1, 1),
        Pair(1, 2),
        Pair(1, 3),
        Pair(1, 5),
        Pair(1, 6),
        Pair(1, 8),
        Pair(2, 0),
        Pair(2, 2),
        Pair(2, 4),
        Pair(2, 7),
        Pair(3, 0),
        Pair(3, 3),
        Pair(3, 6),
        Pair(3, 9),
        Pair(4, 3),
        Pair(4, 7),
        Pair(5, 1),
        Pair(5, 6),
        Pair(6, 2),
        Pair(6, 8),
        Pair(7, 5),
        Pair(8, 2),
        Pair(9, 1)
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Put the app icon in the app bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if ((applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
            supportActionBar?.setIcon(R.drawable.app_icon)
        } else {
            supportActionBar?.setIcon(R.drawable.app_icon_dark)
        }
        supportActionBar?.setDisplayUseLogoEnabled(true)

        // View references
        screen = findViewById(R.id.screen)
        resistorBody = findViewById(R.id.resistor_body)
        ohmsTextView = findViewById(R.id.ohms_text_view)
        band1 = findViewById(R.id.band_1)
        band2 = findViewById(R.id.band_2)
        bandMultiplier = findViewById(R.id.band_3)
        bandLast = findViewById(R.id.band_last)

        // Needed to get the bitmaps later
        band1.isDrawingCacheEnabled = true
        band2.isDrawingCacheEnabled = true
        bandMultiplier.isDrawingCacheEnabled = true
        bandLast.isDrawingCacheEnabled = true

        // On touch listener
        screen.setOnTouchListener { _, m ->
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchedBand = -1
                    hadNoLongPress = true
                    if (!checkBandLast(m)) {
                        if (!bandClicked(m, bandMultiplier)) {
                            if (!bandClicked(m, band2)) {
                                bandClicked(m, band1)
                            }
                        }
                    }
                    previousX = m.x
                }
                MotionEvent.ACTION_UP -> {
                    if (hadNoLongPress) {
                        when (touchedBand) {
                            0 -> nextColor(band1State)
                            1 -> nextColor(band2State)
                            2 -> nextMultiplierColor(bandMultiplierState)
                            -1 -> {
                                if (previousX - m.x > MIN_DISTANCE) {
                                    var goBackOne = false
                                    val prevPair = when (bandLastState) {
                                        ToleranceBandColors.SILVER -> {
                                            try {
                                                e12.dropLastWhile { (it.first > band1State.value.ordinal) or ((it.first == band1State.value.ordinal) and (it.second >= band2State.value.ordinal)) }.last()
                                            } catch (e: NoSuchElementException) {
                                                goBackOne = true
                                                e12.last()
                                            }
                                        }
                                        ToleranceBandColors.GOLD -> {
                                            try {
                                                e24.dropLastWhile { (it.first > band1State.value.ordinal) or ((it.first == band1State.value.ordinal) and (it.second >= band2State.value.ordinal)) }.last()
                                            } catch (e: NoSuchElementException) {
                                                goBackOne = true
                                                e24.last()
                                            }
                                        }
                                        else -> Pair(1, 0)
                                    }
                                    band1State.value = BandColors.values()[prevPair.first]
                                    band2State.value = BandColors.values()[prevPair.second]
                                    if (goBackOne) {
                                        prevMultiplierColor(bandMultiplierState)
                                    }
                                } else if (m.x - previousX > MIN_DISTANCE) {
                                    val nextPair = try {
                                        when (bandLastState) {
                                            ToleranceBandColors.SILVER -> {
                                                e12.dropWhile { (it.first < band1State.value.ordinal) or ((it.first == band1State.value.ordinal) and (it.second <= band2State.value.ordinal)) }[0]
                                            }
                                            ToleranceBandColors.GOLD -> {
                                                e24.dropWhile { (it.first < band1State.value.ordinal) or ((it.first == band1State.value.ordinal) and (it.second <= band2State.value.ordinal)) }[0]
                                            }
                                            else -> Pair(1, 0)
                                        }
                                    } catch (e: IndexOutOfBoundsException) {
                                        Pair(1, 0)
                                    }
                                    band1State.value = BandColors.values()[nextPair.first]
                                    band2State.value = BandColors.values()[nextPair.second]
                                    if (nextPair == Pair(1, 0)) {
                                        nextMultiplierColor(bandMultiplierState)
                                    }
                                }
                            }
                        }
                        updateAll()
                    }
                }
            }

            false
        }

        // On long click listener
        screen.setOnLongClickListener {
            hadNoLongPress = false
            when (touchedBand) {
                0 -> showBandPopup(band1, band1State)
                1 -> showBandPopup(band2, band2State)
                2 -> showMultiplierBandPopup(bandMultiplier, bandMultiplierState)
            }
            true
        }
    }

    private fun bandClicked(m: MotionEvent, band: ImageButton): Boolean {
        return try {
            if (Bitmap.createBitmap(band.drawingCache)
                    .getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
                touchedBand = when (band) {
                    band1 -> 0
                    band2 -> 1
                    bandMultiplier -> 2
                    else -> -1
                }
                true
            } else {
                false
            }
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    // Check if the last band was clicked
    private fun checkBandLast(m: MotionEvent): Boolean {
        return if (bandClicked(m, bandLast)) {
            bandLastState = nextToleranceColor(bandLastState)
            bandLast.setColorFilter(bandLastState.argb)
            resistorBody.setColorFilter(decodeBodyColor().argb)
            decodeOhms()
            touchedBand = 3
            true
        } else {
            false
        }
    }

    // Convert band color states to a string to display
    @SuppressLint("SetTextI18n")
    private fun decodeOhms() {
        var ohms = (((band1State.value.ordinal * 10) + band2State.value.ordinal) * (10.0.pow(bandMultiplierState.value.ordinal - 3)))
        val multiplier =
            when (floor(log10(ohms)) + 1) {
                in Double.NEGATIVE_INFINITY..3.0 -> {
                    ""
                }
                in 4.0..6.0 -> {
                    ohms /= 1000
                    "K"
                }
                in 7.0..9.0 -> {
                    ohms /= 1000000
                    "M"
                }
                else -> {
                    ohms /= 1000000000
                    "G"
                }
            }
        val tolerance =
            when (bandLastState) {
                ToleranceBandColors.SILVER -> "10"
                ToleranceBandColors.GOLD -> "5"
                else -> "?"
            }
        ohmsTextView.text = "${DecimalFormat("0.###").format(ohms)} ${multiplier}Ω ±${tolerance}%"
    }

    // Cycle through colors
    private fun nextColor(bandState: Band) {
        bandState.value = try {
            BandColors.values()[bandState.value.ordinal + 1]
        } catch (e: ArrayIndexOutOfBoundsException) {
            BandColors.values().first()
        }
    }

    private fun nextMultiplierColor(multiplierBandState: MultiplierBand) {
        multiplierBandState.value = when (multiplierBandState.value) {
            MultiplierBandColors.BLACK -> MultiplierBandColors.BROWN
            MultiplierBandColors.BROWN -> MultiplierBandColors.RED
            MultiplierBandColors.RED -> MultiplierBandColors.ORANGE
            MultiplierBandColors.ORANGE -> MultiplierBandColors.YELLOW
            MultiplierBandColors.YELLOW -> MultiplierBandColors.GREEN
            MultiplierBandColors.GREEN -> MultiplierBandColors.BLUE
            MultiplierBandColors.BLUE -> MultiplierBandColors.VIOLET
            MultiplierBandColors.VIOLET -> MultiplierBandColors.BLACK
            else -> MultiplierBandColors.BLACK
        }
    }

    private fun prevMultiplierColor(multiplierBandState: MultiplierBand) {
        multiplierBandState.value = when (multiplierBandState.value) {
            MultiplierBandColors.VIOLET -> MultiplierBandColors.BLUE
            MultiplierBandColors.BLUE -> MultiplierBandColors.GREEN
            MultiplierBandColors.GREEN -> MultiplierBandColors.YELLOW
            MultiplierBandColors.YELLOW -> MultiplierBandColors.ORANGE
            MultiplierBandColors.ORANGE -> MultiplierBandColors.RED
            MultiplierBandColors.RED -> MultiplierBandColors.BROWN
            MultiplierBandColors.BROWN -> MultiplierBandColors.BLACK
            MultiplierBandColors.BLACK -> MultiplierBandColors.VIOLET
            else -> MultiplierBandColors.BLACK
        }
    }

    private fun nextToleranceColor(toleranceBandState: ToleranceBandColors): ToleranceBandColors =
        when (toleranceBandState) {
            ToleranceBandColors.SILVER -> ToleranceBandColors.GOLD
            ToleranceBandColors.GOLD -> ToleranceBandColors.SILVER
            else -> toleranceBandState
        }

    private fun decodeBodyColor(): BodyColors =
        when (bandLastState) {
            ToleranceBandColors.SILVER, ToleranceBandColors.GOLD -> BodyColors.BEIGE
            else -> BodyColors.BLUE
        }

    // Show popup menu on view
    private fun showBandPopup(band: ImageButton, bandState: Band) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.band_numbers)

        popup.setOnMenuItemClickListener {
            bandState.value = when (it!!.itemId) {
                R.id.bandBlack -> BandColors.BLACK
                R.id.bandBrown -> BandColors.BROWN
                R.id.bandRed -> BandColors.RED
                R.id.bandOrange -> BandColors.ORANGE
                R.id.bandYellow -> BandColors.YELLOW
                R.id.bandGreen -> BandColors.GREEN
                R.id.bandBlue -> BandColors.BLUE
                R.id.bandViolet -> BandColors.VIOLET
                R.id.bandGrey -> BandColors.GREY
                R.id.bandWhite -> BandColors.WHITE
                else -> bandState.value
            }
            update(band, bandState)
            true
        }

        popup.show()
    }

    private fun showMultiplierBandPopup(band: ImageButton, bandState: MultiplierBand) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.multiplier_band_numbers)

        popup.setOnMenuItemClickListener {
            bandState.value = when (it!!.itemId) {
                R.id.multiplierBandPink -> MultiplierBandColors.PINK
                R.id.multiplierBandSliver -> MultiplierBandColors.SILVER
                R.id.multiplierBandGold -> MultiplierBandColors.GOLD
                R.id.multiplierBandBlack -> MultiplierBandColors.BLACK
                R.id.multiplierBandBrown -> MultiplierBandColors.BROWN
                R.id.multiplierBandRed -> MultiplierBandColors.RED
                R.id.multiplierBandOrange -> MultiplierBandColors.ORANGE
                R.id.multiplierBandYellow -> MultiplierBandColors.YELLOW
                R.id.multiplierBandGreen -> MultiplierBandColors.GREEN
                R.id.multiplierBandBlue -> MultiplierBandColors.BLUE
                R.id.multiplierBandViolet -> MultiplierBandColors.VIOLET
                R.id.multiplierBandGrey -> MultiplierBandColors.GREY
                R.id.multiplierBandWhite -> MultiplierBandColors.WHITE
                else -> bandState.value
            }
            update(band, bandState)
            true
        }

        popup.show()
    }

    // Update bands and text
    private fun update(band: ImageButton, bandState: Band) {
        band.setColorFilter(bandState.value.argb)
        decodeOhms()
    }
    private fun update(band: ImageButton, bandState: MultiplierBand) {
        band.setColorFilter(bandState.value.argb)
        decodeOhms()
    }

    // Update all bands and texts
    private fun updateAll() {
        update(band1, band1State)
        update(band2, band2State)
        update(bandMultiplier, bandMultiplierState)
    }
}