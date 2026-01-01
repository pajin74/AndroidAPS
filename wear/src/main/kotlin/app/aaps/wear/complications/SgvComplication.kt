package app.aaps.wear.complications

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountUpTimeReference
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.wear.R
import app.aaps.wear.interaction.utils.SmallestDoubleString
import dagger.android.AndroidInjection
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.get

/**
 * SGV (Sensor Glucose Value) Complication
 *
 * Shows current blood glucose with arrow, auto-updating time, and delta
 * Display format: "6.8↘" with "5m Δ-3" above
 * - Time auto-updates every minute (battery efficient)
 * - Delta is static until new BG reading
 */
class SgvComplication : ModernBaseComplicationProviderService() {

    @Inject lateinit var sp: SP

    // Not derived from DaggerService, do injection here
    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun buildComplicationData(
        type: ComplicationType,
        data: app.aaps.wear.data.ComplicationData,
        complicationPendingIntent: PendingIntent
    ): ComplicationData? {
        // Use dataset 0 (primary)
        val bgData = data.bgData

        // Main text: BG value + arrow (with variation selector to prevent emoji)
        val mainText = bgData.sgvString + bgData.slopeArrow //+ "\uFE0E"

        // Title: auto-updating time + delta (e.g., "5m Δ-3")
        val minutes = displayFormat.shortTimeSince(bgData.timeStamp)
        val titleText = "\u0394" + SmallestDoubleString(bgData.delta).minimise(4) + " (" + minutes + ")"
        val titleTextCom = buildDeltaAndTimeTitle(bgData)

        aapsLogger.debug(LTag.WEAR, "SgvComplication building: dataset=0 sgv=${bgData.sgvString} (${bgData.sgv}) arrow=${bgData.slopeArrow} maintext=${mainText} gbData.delta=${bgData.delta} (${bgData.deltaDetailed}) bgData.timeStamp=${bgData.timeStamp}")

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                buildShortTextComplication(bgData, complicationPendingIntent)
            }

            ComplicationType.RANGED_VALUE        -> {
                val builder = RangedValueComplicationData.Builder(
                    value = bgData.sgv.toFloat(),
                    min = 0f,
                    max = 300f,
                    contentDescription = PlainComplicationText.Builder(text = "Glucose " + mainText).build()
                )
                    .setText(PlainComplicationText.Builder(text = mainText).build())
                    //.setTitle(PlainComplicationText.Builder(text = titleText.toString()).build())
                    .setTitle(titleTextCom)
                builder.setTapAction(complicationPendingIntent).build()
            }

            // ComplicationType.RANGED_VALUE -> {
            //    val builder = RangedValueComplicationData.Builder()
            //        .setMinValue(raw.singleBg[0].low.toFloat())
            //        .setMaxValue(raw.singleBg[0].high.toFloat())
            //        .setValue(sgv.toFloat())
            //        .setShortText(ComplicationText.plainText(shortText))
            //         .setShortTitle(ComplicationText.plainText( displayFormat.shortTrend(raw, 0)))
            //         .setTapAction(complicationPendingIntent)
            //     complicationData = builder.build()
            // }

            else -> {
                aapsLogger.warn(LTag.WEAR, "SgvComplication unexpected type: $type")
                null
            }
        }
    }

    private fun buildShortTextComplication(
        bgData: app.aaps.core.interfaces.rx.weardata.EventData.SingleBg,
        pendingIntent: PendingIntent
    ): ShortTextComplicationData {
        // Main text: BG value + arrow (with variation selector to prevent emoji)
        val mainText = bgData.sgvString + bgData.slopeArrow + "\uFE0E"

        // Title: auto-updating time + delta (e.g., "5m Δ-3")
        val titleText = buildDeltaAndTimeTitle(bgData)

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = mainText).build(),
            contentDescription = PlainComplicationText.Builder(text = "Glucose $mainText").build()
        )
            .setTitle(titleText)
            .setTapAction(pendingIntent)
            .build()
    }

    /**
     * Build combined delta and time title (e.g., "5m Δ-3")
     * Time auto-updates, delta is static
     * Uses ^1 placeholder which is replaced with the time difference
     */
    private fun buildDeltaAndTimeTitle(bgData: app.aaps.core.interfaces.rx.weardata.EventData.SingleBg): TimeDifferenceComplicationText {
        // Use detailed delta if preference is enabled, otherwise use simple delta
        val rawDelta = if (sp.getBoolean(app.aaps.wear.R.string.key_show_detailed_delta, false)) {
            bgData.deltaDetailed
        } else {
            bgData.delta
        }

        // Add delta symbol if Unicode complications are enabled
        val useUnicode = sp.getBoolean("complication_unicode", true)
        val deltaSymbol = if (useUnicode) "\u0394" else ""

        // Minimize delta to leave room for time (max 7 chars total for SHORT_TEXT title)
        val deltaText = deltaSymbol + SmallestDoubleString(rawDelta).minimise(4)
        //val deltaText = SmallestDoubleString(rawDelta).minimise(3)

        // ^1 is replaced with auto-updating time (e.g., "5m")
        // Format: "5m Δ-3" where time auto-updates every minute
        return TimeDifferenceComplicationText.Builder(
            style =  TimeDifferenceStyle.SHORT_SINGLE_UNIT,
            countUpTimeReference = CountUpTimeReference(Instant.ofEpochMilli(bgData.timeStamp))
        )
            .setMinimumTimeUnit(TimeUnit.MINUTES)
            .setText("$deltaText (^1)")
            .build()
    }

    override fun getProviderCanonicalName(): String = SgvComplication::class.java.canonicalName!!
}