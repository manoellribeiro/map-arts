package manoellribeiro.dev.martp.core.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.setPadding
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.databinding.MartpButtonEndIconBinding
import manoellribeiro.dev.martp.databinding.MartpButtonStartIconBinding
import manoellribeiro.dev.martp.databinding.MartpButtonTitleTextBinding

class MartpButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var title: String = ""
        set(value) {
            field = value
            titleBinding.titleTX.text = value
        }
    private var customIsEnabled: Boolean
    private var fontAndIconColor: Int
    private var buttonBackgroundColor: Int
    private var strokeColor: Int
    private var cornerRadius: Float
    private var martpButtonType: MartpButtonType

    val progressBar = ProgressBar(context).apply {
        layoutParams = LayoutParams(70, 70).apply {
            bottomToBottom = ConstraintSet.PARENT_ID
            endToEnd = ConstraintSet.PARENT_ID
            startToStart = ConstraintSet.PARENT_ID
            topToTop = ConstraintSet.PARENT_ID
        }
    }

    val startIconBinding = MartpButtonStartIconBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    val endIconBinding = MartpButtonEndIconBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    val titleBinding = MartpButtonTitleTextBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {

        val customAttributes = context.obtainStyledAttributes(attrs, R.styleable.MartpButton)
        fontAndIconColor = customAttributes.getColor(
            R.styleable.MartpButton_fontAndIconColor,
            ContextCompat.getColor(context, R.color.white)
        )

        val titleText = customAttributes.getString(R.styleable.MartpButton_title) ?: ""


        val startIconDrawable = customAttributes.getDrawable(R.styleable.MartpButton_startIcon)
        val endIconDrawable = customAttributes.getDrawable(R.styleable.MartpButton_endIcon)
        customIsEnabled = customAttributes.getBoolean(R.styleable.MartpButton_isEnabled, true)
        cornerRadius = customAttributes.getDimension(
            R.styleable.MartpButton_cornerRadius,
            8.0F
        )
        buttonBackgroundColor = customAttributes.getColor(
            R.styleable.MartpButton_buttonBackgroundColor,
            ContextCompat.getColor(context, R.color.white)
        )
        strokeColor = customAttributes.getColor(
            R.styleable.MartpButton_strokeColor,
            ContextCompat.getColor(context, R.color.white)
        )

        martpButtonType = MartpButtonType.values()[
            customAttributes.getInt(R.styleable.MartpButton_type, 0)
        ]

        customAttributes.recycle()
        initViews(
            titleText,
            fontAndIconColor,
            startIconDrawable,
            endIconDrawable,
            buttonBackgroundColor,
            strokeColor,
            cornerRadius,
            martpButtonType,
        )
    }

    fun setTitleColor(color: Int) {
        titleBinding.titleTX.setTextColor(color)
    }

    fun setButtonBackGroundColor(color: Int) {
        (
            (this@MartpButton.background as RippleDrawable)
                .findDrawableByLayerId(R.id.martpButtonShape) as GradientDrawable
            ).apply {
                setColor(color)
            }
        buttonBackgroundColor = color
    }

    fun setStrokeColor(color: Int) {
        (
            (this@MartpButton.background as RippleDrawable)
                .findDrawableByLayerId(R.id.martpButtonShape) as GradientDrawable
            ).apply {
                setStroke(
                    resources.getDimension(R.dimen.border_width_hairline).toInt(),
                    color
                )
            }
        strokeColor = color
    }

    private fun initViews(
        title: String,
        fontAndIconColor: Int,
        startIcon: Drawable?,
        endIcon: Drawable?,
        buttonBackgroundColor: Int,
        strokeColor: Int,
        cornerRadius: Float,
        martpButtonType: MartpButtonType,
    ) {
        this@MartpButton.setPadding(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8F, resources.displayMetrics).toInt() +
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4F, resources.displayMetrics).toInt()
        )

        this@MartpButton.title = title
        titleBinding.titleTX.setTextColor(fontAndIconColor)

        this@MartpButton.background = when (martpButtonType) {
            MartpButtonType.FILLED -> ContextCompat.getDrawable(
                context,
                R.drawable.background_martp_button_background_filled
            )

            MartpButtonType.OUTLINED -> ContextCompat.getDrawable(
                context,
                R.drawable.background_martp_button_background_outlined
            )
        }

        (
            (this@MartpButton.background as RippleDrawable)
                .findDrawableByLayerId(R.id.martpButtonShape) as GradientDrawable
            ).apply {
                setStroke(
                    resources.getDimension(R.dimen.border_width_hairline).toInt(),
                    strokeColor
                )
                setCornerRadius(cornerRadius)
                setColor(buttonBackgroundColor)
            }

        if (startIcon != null) {
            startIconBinding.startIconIMG.visibility = VISIBLE
            startIconBinding.startIconIMG.setImageDrawable(startIcon)
            startIconBinding.startIconIMG.imageTintList = ColorStateList.valueOf(fontAndIconColor)
        }

        if (endIcon != null) {
            endIconBinding.endIconIMG.visibility = VISIBLE
            endIconBinding.endIconIMG.setImageDrawable(endIcon)
            endIconBinding.endIconIMG.imageTintList = ColorStateList.valueOf(fontAndIconColor)
        }

        toggleActiveState(customIsEnabled)
    }

    private fun toggleActiveState(isActive: Boolean) {
        if (!isActive) {
            (
                (this@MartpButton.background as RippleDrawable)
                    .findDrawableByLayerId(R.id.martpButtonShape) as GradientDrawable
                ).apply {
                    val lightL2Color = ContextCompat.getColor(context, R.color.light_l2)
                    setStroke(
                        resources.getDimension(R.dimen.border_width_hairline).toInt(),
                        lightL2Color
                    )
                    when (martpButtonType) {
                        MartpButtonType.FILLED -> setColor(lightL2Color)
                        MartpButtonType.OUTLINED -> setColor(Color.WHITE)
                    }
                }

            titleBinding.titleTX.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.dark_d4
                )
            )
        } else {
            titleBinding.titleTX.setTextColor(fontAndIconColor)
            (
                (this@MartpButton.background as RippleDrawable)
                    .findDrawableByLayerId(R.id.martpButtonShape) as GradientDrawable
                ).apply {
                    setStroke(
                        resources.getDimension(R.dimen.border_width_hairline).toInt(),
                        strokeColor
                    )
                    setColor(buttonBackgroundColor)
                }
        }
    }

    fun enableLoadingState(progressBarColor: Int) {
        startIconBinding.startIconIMG.visibility = GONE
        endIconBinding.endIconIMG.visibility = GONE
        titleBinding.titleTX.visibility = GONE
        this.isClickable = false
        progressBar.indeterminateDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                progressBarColor,
                BlendModeCompat.SRC_ATOP
            )

        if (this.parent != null) {
            this.removeView(progressBar)
        }

        this.addView(progressBar)
    }

    fun disableLoadingState() {
        startIconBinding.startIconIMG.visibility = VISIBLE
        endIconBinding.endIconIMG.visibility = VISIBLE
        titleBinding.titleTX.visibility = VISIBLE
        this.isClickable = true
        this.removeView(progressBar)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        customIsEnabled = enabled
        toggleActiveState(customIsEnabled)
    }

    private enum class MartpButtonType {
        FILLED,
        OUTLINED
    }
}