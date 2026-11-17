package mx.edu.agrotech

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprobar si existe una sesión activa
        if (SupabaseClientProvider.client.auth.currentSessionOrNull() != null) {
            openHome()
            return
        }

        buildLogin()
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
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or
                    android.text.InputType.TYPE_CLASS_TEXT
            background = rounded(Color.WHITE)
            setPadding(dp(15), dp(14), dp(15), dp(14))
        }

        root.addView(emailField, params())

        passwordField = EditText(this).apply {
            hint = "Contraseña"
            textSize = 15f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = rounded(Color.WHITE)
            setPadding(dp(15), dp(14), dp(15), dp(14))
        }

        root.addView(passwordField, params())

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

                openHome()

            } catch (e: Exception) {

                message.text = "Correo o contraseña incorrectos. Verifica tus datos e inténtalo nuevamente."

            }
            finally {

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
