package app.aaps.wear.complications

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationText
import androidx.annotation.DrawableRes
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import app.aaps.core.interfaces.logging.LTag
import app.aaps.wear.R
import dagger.android.AndroidInjection
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Uploader Battery Complication
 *
 * Shows phone battery level for uploader device
 *
 */
class ReservoirIconComplication : ModernBaseComplicationProviderService() {

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
            ComplicationType.RANGED_VALUE        -> {
                val builder = RangedValueComplicationData.Builder(
                    value = statusData.reservoir.toFloat(),
                    min = 0f,
                    max = 300f,
                    contentDescription = PlainComplicationText.Builder(text = "Reservoir " + statusData.reservoirString).build()
                )
                    .setText(PlainComplicationText.Builder(text = statusData.reservoirString).build())
                    .setMonochromaticImage(MonochromaticImage.Builder(image = Icon.createWithResource(this, R.drawable.ic_ins)).build())
                    builder.setTapAction(complicationPendingIntent).build()
            }

            else                               -> {
                aapsLogger.warn(LTag.WEAR, "Unexpected complication type $type")
                null
            }
        }
    }

    override fun getProviderCanonicalName(): String = ReservoirIconComplication::class.java.canonicalName!!
}
