package app.aaps.wear.complications

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import app.aaps.core.interfaces.logging.LTag
import app.aaps.wear.R
import dagger.android.AndroidInjection

/**
 * Basal Rate (BR) Complication
 *
 * Shows current basal rate with basal symbol
 *
 */
class TmpTargetComplication : ModernBaseComplicationProviderService() {

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
        val statusData = data.statusData

        return when (type) {
            ComplicationType.SHORT_TEXT      -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text = statusData.tempTarget).build(),
                    contentDescription = PlainComplicationText.Builder(text = "Temp Target").build()
                )
                    .setTitle(PlainComplicationText.Builder(text = statusData.tempTargetLevel.toString()).build())
                    .setTapAction(complicationPendingIntent)
                    .build()
            }

            else                             -> {
                aapsLogger.warn(LTag.WEAR, "Unexpected complication type $type")
                null
            }
        }
    }

    override fun getProviderCanonicalName(): String = TmpTargetComplication::class.java.canonicalName!!
}