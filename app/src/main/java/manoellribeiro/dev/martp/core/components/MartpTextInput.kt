package manoellribeiro.dev.martp.core.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import manoellribeiro.dev.martp.R
import manoellribeiro.dev.martp.databinding.MartpTextInputBinding
import kotlin.properties.Delegates

/**
 *  Martp Input Text
 *
 *  - You can set a validation listener using the method [setValidationListener]
 *  - You can set a text changed listener using the method [setOnChangedTextListener]
 *
 *  @param placeholder text to appear on input box before user start to write.
 *  @param placeholderColor placer holder text color.
 *  @param labelName text to always appear on top of text field box.
 *  @param labelColor label text color.
 *  @param inputBackgroundColor color to be shown as input background.
 *  @param strokeColor color to be shown on input stroke.
 *  @param icon icon to appear on the end of text field box.
 *  @param isObfuscated flag that indicates if text is obfuscated.
 *
 */
class MartpTextInput(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(
    context,
    attrs
) {

    private var placeholder: String?
    var hint: String?
        get() = inputBinding.textInputLayout.hint.toString()
        set(value) {
            inputBinding.textInputEditText.hint = value
        }

    var inputType: Int
        get() = inputBinding.textInputEditText.inputType
        set(value) {
            inputBinding.textInputEditText.inputType = value
        }

    private var icon: Drawable?
    private var validationListener: ((onChangedText: String) -> String?)? = null
    private var onChangedTextListener: ((onChangedText: String) -> Unit)? = null
    var currentText: String
        get() = inputBinding.textInputEditText.text.toString()
        set(value) {
            inputBinding.textInputEditText.setText(value)
        }
    var isObfuscated: Boolean
        get() = inputBinding.textInputEditText.transformationMethod is PasswordTransformationMethod

    private var iconPosition: IconPosition
    private var inputBackgroundColor: Int
    private var iconColor by Delegates.notNull<Int>()
    private var label: String

    private val errorMediumColor by lazy {
        ContextCompat.getColor(context, R.color.error_medium)
    }
    private val darkD2Color by lazy {
        ContextCompat.getColor(context, R.color.dark_d2)
    }

    private val inputBinding = MartpTextInputBinding.inflate(
        LayoutInflater.from(
            context
        ),
        this,
        false
    )

    private lateinit var mInputEditText: EditText

    private var inputTextWatcher: TextWatcher? = null
    private var callbackTextWatcher: TextWatcher? = null

    init {
        inputBinding.textInputLayout.editText?.apply {
            mInputEditText = this
        }
        val customAttributes = context.obtainStyledAttributes(attrs, R.styleable.MartpTextInput)

        val obfuscatedAttribute = customAttributes.getBoolean(
            R.styleable.MartpTextInput_obfuscated,
            false
        )
        isObfuscated = obfuscatedAttribute

        iconPosition = IconPosition.values()[
            customAttributes.getInt(
                R.styleable.MartpTextInput_iconPosition,
                1
            )
        ]

        val minLines = customAttributes.getInt(
            R.styleable.MartpTextInput_android_minLines,
            1
        )

        val gravity = customAttributes.getInt(
            R.styleable.MartpTextInput_android_gravity,
            Gravity.START or Gravity.CENTER
        )

        val imeOption = customAttributes.getInt(
            R.styleable.MartpTextInput_android_imeOptions,
            EditorInfo.IME_ACTION_UNSPECIFIED
        )

        val inputType = customAttributes.getInt(
            R.styleable.MartpTextInput_android_inputType,
            InputType.TYPE_CLASS_TEXT
        )

        placeholder = customAttributes.getString(R.styleable.MartpTextInput_placeholder)
        val placeholderColor = customAttributes.getColor(
            R.styleable.MartpTextInput_placeholderColor,
            ContextCompat.getColor(context, R.color.dark_d3)
        )

        val defaultCornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8.0F,
            context.resources.displayMetrics
        )

        val cornerRadius = customAttributes.getDimension(
            R.styleable.MartpTextInput_cornerRadius,
            defaultCornerRadius
        )

        hint = customAttributes.getString(R.styleable.MartpTextInput_hint)
        val hintColor = customAttributes.getColor(
            R.styleable.MartpTextInput_hintColor,
            ContextCompat.getColor(context, R.color.dark_d3)
        )
        inputBackgroundColor = customAttributes.getColor(
            R.styleable.MartpTextInput_inputBackgroundColor,
            ContextCompat.getColor(context, R.color.light_l5)
        )
        val strokeColor = customAttributes.getColor(
            R.styleable.MartpTextInput_strokeColor,
            ContextCompat.getColor(context, R.color.blue)
        )
        icon = customAttributes.getDrawable(R.styleable.MartpTextInput_icon)

        iconColor = customAttributes.getColor(
            R.styleable.MartpTextInput_iconColor,
            ContextCompat.getColor(context, R.color.dark_d2)
        )

        label = customAttributes.getString(R.styleable.MartpTextInput_label).orEmpty()

        customAttributes.recycle()
        initViews(
            label = label,
            placeholder = placeholder,
            placeholderColor = placeholderColor,
            hint = hint,
            hintColor = hintColor,
            backgroundColor = inputBackgroundColor,
            strokeColor = strokeColor,
            icon = icon,
            obfuscated = obfuscatedAttribute,
            iconPosition = iconPosition,
            iconColor = iconColor,
            cornerRadius = cornerRadius,
            imeOption = imeOption,
            inputType = inputType,
            minLines = minLines,
            gravity = gravity
        )
    }

    private fun initViews(
        label: String?,
        placeholder: String?,
        placeholderColor: Int,
        hint: String?,
        hintColor: Int,
        backgroundColor: Int,
        strokeColor: Int,
        icon: Drawable?,
        obfuscated: Boolean,
        iconPosition: IconPosition,
        iconColor: Int,
        cornerRadius: Float,
        imeOption: Int,
        inputType: Int,
        minLines: Int,
        gravity: Int
    ) {
        with(inputBinding) {
            if (label.isNullOrEmpty()) {
                textView.visibility = View.GONE
                space.visibility = View.GONE
            } else {
                textView.text = label
            }

            if (hint.isNullOrEmpty()) {
                textInputLayout.placeholderText = placeholder
                textInputLayout.placeholderTextColor = ColorStateList.valueOf(placeholderColor)
            }
            textInputLayout.hint = hint
            textInputLayout.hintTextColor = ColorStateList.valueOf(hintColor)
            textInputEditText.setPadding(
                textInputEditText.paddingLeft,
                0,
                textInputEditText.paddingRight,
                0
            )
            textInputEditText.minLines = minLines
            textInputEditText.gravity = gravity
            textInputEditText.imeOptions = imeOption
            this@MartpTextInput.inputType = inputType
            textInputEditText.inputType = inputType
            when (iconPosition) {
                IconPosition.START -> {
                    textInputLayout.startIconDrawable = icon
                    textInputLayout.setStartIconTintList(ColorStateList.valueOf(iconColor))
                    textInputLayout.isStartIconVisible = true
                }
                IconPosition.END -> {
                    textInputLayout.endIconDrawable = icon?.apply { setTint(iconColor) }
                    textInputLayout.setEndIconTintList(ColorStateList.valueOf(iconColor))
                    textInputLayout.isEndIconVisible = true
                }
            }
            textInputLayout.boxStrokeColor = strokeColor
            textInputLayout.boxStrokeWidth = 2
            textInputLayout.boxBackgroundColor = backgroundColor

            textInputLayout.setBoxCornerRadii(
                cornerRadius,
                cornerRadius,
                cornerRadius,
                cornerRadius
            )
        }
        setObfuscateMode(obfuscated)

        this.addView(inputBinding.root)
    }

    fun setLabel(label: String?) {
        if (label.isNullOrEmpty()) {
            inputBinding.textView.visibility = View.GONE
            inputBinding.space.visibility = View.GONE
        } else {
            inputBinding.textView.visibility = View.VISIBLE
            inputBinding.space.visibility = View.VISIBLE
            inputBinding.textView.text = label
        }
    }

    override fun setFocusable(isFocusable: Boolean) {
        inputBinding.textInputEditText.isFocusable = isFocusable
    }

    fun setOnEditorActionListener(listener: TextView.OnEditorActionListener?) {
        inputBinding.textInputEditText.setOnEditorActionListener(listener)
    }

    fun removeTextChangedListener(watcher: TextWatcher) {
        if (callbackTextWatcher == watcher) {
            callbackTextWatcher = null
        }
    }

    fun setTransformationMethod(method: TransformationMethod?) {
        inputBinding.textInputEditText.transformationMethod = method
    }

    private fun setErrorState() {
        inputBinding.textView.setTextColor(errorMediumColor)
        inputBinding.textInputLayout.isErrorEnabled = true
        inputBinding.textInputLayout.boxBackgroundColor = ContextCompat.getColor(
            context,
            R.color.error_lightest
        )
        when (iconPosition) {
            IconPosition.START -> {
                inputBinding.textInputLayout.setStartIconTintList(
                    ColorStateList.valueOf(
                        errorMediumColor
                    )
                )
            }
            IconPosition.END -> {
                inputBinding.textInputLayout.setEndIconTintList(
                    ColorStateList.valueOf(
                        errorMediumColor
                    )
                )
            }
        }
    }

    private fun removeErrorState() {
        inputBinding.textView.setTextColor(darkD2Color)
        inputBinding.textInputLayout.isErrorEnabled = false
        inputBinding.textInputLayout.boxBackgroundColor = inputBackgroundColor
        when (iconPosition) {
            IconPosition.START -> {
                inputBinding.textInputLayout.setStartIconTintList(ColorStateList.valueOf(iconColor))
            }
            IconPosition.END -> {
                inputBinding.textInputLayout.setEndIconTintList(ColorStateList.valueOf(iconColor))
            }
        }
    }

    /**
     *  Add an error message to edit text
     */
    fun setError(errorMessage: String?) {
        inputBinding.textInputLayout.error = errorMessage
        if (errorMessage != null) {
            setErrorState()
        } else {
            removeErrorState()
        }
    }

    /**
     *  Change obfuscated mode programmatically
     */
    fun setObfuscateMode(shouldObfuscate: Boolean) {
        inputBinding.textInputEditText.transformationMethod = if (shouldObfuscate) {
            PasswordTransformationMethod.getInstance()
        } else {
            null
        }
    }

    /**
     *  Change end icon programmatically
     */
    fun setEndIcon(icon: Drawable?) {
        inputBinding.textInputLayout.endIconDrawable = icon
    }

    /**
     *  Set a block of code to run everytime user taps end icon
     */
    fun setOnClickIconListener(listener: (() -> Unit)?) {
        inputBinding.textInputLayout.setEndIconOnClickListener {
            listener?.invoke()
        }
    }

    /**
     *  Set a block of code to run everytime text changes
     */
    fun setOnChangedTextListener(
        textWatcher: TextWatcher? = null,
        listener: ((onChangedText: String) -> Unit)? = null
    ) {
        onChangedTextListener = listener
        callbackTextWatcher = textWatcher
        if (inputTextWatcher == null) {
            inputTextWatcher = inputBinding.textInputEditText.addTextChangedListener(
                onTextChanged = { text, start, before, count ->
                    callbackTextWatcher?.onTextChanged(text, start, before, count)
                },
                beforeTextChanged = { text, start, before, count ->
                    callbackTextWatcher?.beforeTextChanged(text, start, before, count)
                },
                afterTextChanged = { text: Editable? ->
                    callbackTextWatcher?.afterTextChanged(text)
                    onChangedTextListener?.invoke(text.toString())
                    inputBinding.textInputLayout.error = validationListener?.invoke(text.toString())
                }
            )
        }
    }

    /**
     *  Set a block of code to validate text everytime text changes, it must returns a string when
     *  the text is not valid, this string will be showed as error message and should return null
     *  if the text is valid.
     */
    fun setValidationListener(listener: ((onChangedText: String) -> String?)?) {
        validationListener = listener
        inputBinding.textInputEditText.addTextChangedListener(
            afterTextChanged = { text: Editable? ->
                onChangedTextListener?.invoke(text.toString())
                val errorMessage = validationListener?.invoke(text.toString())
                inputBinding.textInputLayout.error = errorMessage
                if (errorMessage != null) {
                    setErrorState()
                } else {
                    removeErrorState()
                }
            }
        )
    }

    private enum class IconPosition {
        START,
        END
    }

    fun setSelection(index: Int) {
        inputBinding.textInputEditText.setSelection(index)
    }

    fun getSelectionStart(): Int {
        return inputBinding.textInputEditText.selectionStart
    }

    fun getInputEditText(): EditText = mInputEditText

    fun hasError() = inputBinding.textInputLayout.isErrorEnabled

    fun showKeyboard() {
        inputBinding.textInputEditText.requestFocus()
        post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputBinding.textInputEditText, 0)
            imm.showSoftInput(inputBinding.textInputEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
