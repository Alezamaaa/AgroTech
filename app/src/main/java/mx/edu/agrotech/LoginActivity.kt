
package mx.edu.agrotech

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.Gravity
import android.widget.*
import android.graphics.drawable.GradientDrawable
import io.github.jan.supabase.auth.providers.builtin.Email
import mx.edu.agrotech.config.SupabaseClientProvider
import kotlinx.coroutines.*
import io.github.jan.supabase.auth.auth

class LoginActivity : Activity() {

    private val forest = Color.rgb(22, 75, 50)
    private val paper = Color.rgb(245, 247, 241)
    private val ink = Color.rgb(25, 55, 41)
    private val muted = Color.rgb(89, 108, 94)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    private val preferences by lazy {
        getSharedPreferences("agrotech_preferences", MODE_PRIVATE)
    }

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var message: TextView
    private lateinit var rememberEmail: CheckBox
    private lateinit var keepSession: CheckBox
    private lateinit var showPasswordButton: ImageButton

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scope.launch {
            try {
                val prefs = getSharedPreferences(
                    "agrotech_preferences",
                    MODE_PRIVATE
                )

                // Esperar a que Supabase recupere la sesión guardada.
                SupabaseClientProvider.client.auth.awaitInitialization()

                val mantenerSesion = prefs.getBoolean("keep_session", false)
                val sesion = SupabaseClientProvider.client.auth.currentSessionOrNull()

                if (sesion != null && mantenerSesion) {
                    openHome()
                } else {
                    if (sesion != null) {
                        SupabaseClientProvider.client.auth.signOut()
                    }

                    buildLogin()
                }
            } catch (e: Exception) {
                buildLogin()
            }
        }
    }

    private fun buildLogin() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(26), dp(30), dp(26), dp(30))
            setBackgroundColor(paper)
        }

        val brand = TextView(this).apply {
            text = "🌱 AGROTECH"
            textSize = 17f
            setTextColor(forest)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(brand)

        val title = TextView(this).apply {
            text = "Bienvenido"
            textSize = 29f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(35), 0, dp(8))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Ingresa a la administración de tu rancho"
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        emailField = EditText(this).apply {
            hint = "Correo electrónico"
            textSize = 15f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            background = rounded(Color.WHITE)
            setPadding(dp(15), dp(14), dp(15), dp(14))

            if (preferences.getBoolean("remember_email", true)) {
                setText(preferences.getString("saved_email", ""))
            }
        }
        root.addView(emailField, params())

        // Campo de contraseña y botón mostrar/ocultar
        val passwordContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.WHITE)
        }

        passwordField = EditText(this).apply {
            hint = "Contraseña"
            textSize = 15f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
            background = null
            setPadding(dp(15), dp(14), dp(8), dp(14))
        }

        passwordContainer.addView(
            passwordField,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        showPasswordButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)

            background = null
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setColorFilter(forest)

            contentDescription = "Mostrar contraseña"
            setPadding(dp(10), 0, dp(15), 0)

            setOnClickListener {
                passwordVisible = !passwordVisible

                if (passwordVisible) {
                    passwordField.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()

                    contentDescription = "Ocultar contraseña"
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                } else {
                    passwordField.transformationMethod =
                        PasswordTransformationMethod.getInstance()

                    contentDescription = "Mostrar contraseña"
                    setImageResource(android.R.drawable.ic_menu_view)
                }

                passwordField.setSelection(passwordField.text.length)
            }
        }

        passwordContainer.addView(
            showPasswordButton,
            LinearLayout.LayoutParams(dp(48), dp(55))
        )

        root.addView(
            passwordContainer,
            params()
        )

        // Recordar correo
        rememberEmail = CheckBox(this).apply {
            text = "Recordar correo electrónico"
            textSize = 13f
            setTextColor(ink)
            buttonTintList = android.content.res.ColorStateList.valueOf(forest)
            isChecked = preferences.getBoolean("remember_email", true)
        }

        root.addView(
            rememberEmail,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
            }
        )

        // Mantener sesión
        keepSession = CheckBox(this).apply {
            text = "Mantener sesión iniciada"
            textSize = 13f
            setTextColor(ink)
            buttonTintList = android.content.res.ColorStateList.valueOf(forest)
            isChecked = preferences.getBoolean("keep_session", true)
        }

        root.addView(
            keepSession,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(4)
            }
        )

        message = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(170, 40, 40))
            setPadding(0, dp(12), 0, dp(5))
        }
        root.addView(message)

        loginButton = Button(this).apply {
            text = "Iniciar sesión"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(forest)
            setOnClickListener { login() }
        }

        root.addView(loginButton, params())

        val footer = TextView(this).apply {
            text = "Acceso exclusivo para usuarios autorizados"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(25), 0, 0)
        }
        root.addView(footer)

        setContentView(root)
    }

    private fun login() {

        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            message.text = "Ingresa un correo electrónico válido."
            return
        }

        if (password.isBlank()) {
            message.text = "Ingresa tu contraseña."
            return
        }

        loginButton.isEnabled = false
        loginButton.text = "Conectando..."
        message.text = ""

        scope.launch {
            try {
                SupabaseClientProvider.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                preferences.edit()
                    .putBoolean("remember_email", rememberEmail.isChecked)
                    .putBoolean("keep_session", keepSession.isChecked)
                    .putString(
                        "saved_email",
                        if (rememberEmail.isChecked) email else ""
                    )
                    .apply()

                openHome()

            } catch (e: Exception) {

                message.text =
                    "Correo o contraseña incorrectos. Verifica tus datos e inténtalo nuevamente."

            } finally {

                loginButton.isEnabled = true
                loginButton.text = "Iniciar sesión"
            }
        }
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun params() =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(55)
        ).apply {
            topMargin = dp(16)
        }

    private fun rounded(color: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.rgb(219, 228, 216))
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
