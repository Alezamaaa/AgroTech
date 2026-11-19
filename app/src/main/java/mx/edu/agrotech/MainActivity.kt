package mx.edu.agrotech

import android.app.Activity
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.*
import mx.edu.agrotech.data.*
import mx.edu.agrotech.ui.LineIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import android.app.AlertDialog
import android.content.Intent
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mx.edu.agrotech.config.SupabaseClientProvider
import android.util.Log
import mx.edu.agrotech.data.SupabaseRepository


/** Interfaz nativa Android en Kotlin. Los módulos de datos no dependen de esta Activity. */
class MainActivity : Activity() {
    private val repo get() = (application as DemoApplication).repository
    private val authScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )
    private val forest = Color.rgb(22, 75, 50)
    private val paper = Color.rgb(245, 247, 241)
    private val ink = Color.rgb(25, 55, 41)
    private val muted = Color.rgb(89, 108, 94)
    private val line = Color.rgb(219, 228, 216)
    private val leaf = Color.rgb(223, 237, 204)
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var nav: LinearLayout
    private lateinit var headerRancho: TextView
    private val stack = mutableListOf<String>()
    private var route = "home"
    private var ranchoRegistrado = false
    private var cargandoDatos = true
    private var nombreRancho = ""
    private var birth: LocalDate? = null
    private var weightDate = LocalDate.now()
    private val displayDate =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX"))
    private var draft = Bundle()

    private val supabaseScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        route = savedInstanceState?.getString("route") ?: "home"
        stack.addAll(savedInstanceState?.getStringArrayList("stack") ?: emptyList())
        draft = savedInstanceState?.getBundle("draft") ?: Bundle()
        birth = savedInstanceState?.getString("birth")?.let(LocalDate::parse)
        weightDate =
            savedInstanceState?.getString("weightDate")?.let(LocalDate::parse) ?: LocalDate.now()

        buildShell()
        render()

        probarConsultaSupabase()
    }

    private fun buildShell() {
        root = column().apply { setBackgroundColor(paper) }
        setContentView(root)
        // Aplica barras del sistema y teclado en Android moderno, incluido edge-to-edge.
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val system =
                    insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                view.setPadding(
                    system.left,
                    system.top,
                    system.right,
                    maxOf(system.bottom, keyboard.bottom)
                )
                nav.visibility =
                    if (insets.isVisible(WindowInsets.Type.ime())) View.GONE else View.VISIBLE
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                )
            }
            insets
        }
        val header = column().apply {
            setPadding(
                dp(24),
                dp(18),
                dp(24),
                dp(19)
            ); setBackgroundColor(forest)
        }
        val brand = row()
        brand.addView(
            LineIcon(this, "sprout", Color.WHITE),
            LinearLayout.LayoutParams(dp(22), dp(22))
        )
        brand.addView(text("AGROTECH", 12, Color.WHITE, true).apply {
            letterSpacing = .16f; setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))

        val settingsButton = row().apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(10), dp(6), dp(4), dp(6))

            addView(
                LineIcon(this@MainActivity, "settings", Color.WHITE),
                LinearLayout.LayoutParams(dp(24), dp(24))
            )

            contentDescription = "Configuración"

            setOnClickListener {
                navigate("settings", true)
            }
        }

        brand.addView(settingsButton)
        header.addView(brand)
        headerRancho = text(nombreRancho.ifBlank { "AgroTech" }, 25, Color.WHITE, true).apply {
            setPadding(0, dp(12), 0, 0)
        }
        header.addView(headerRancho)
        header.addView(text("Ganadería ovina", 14, leaf))
        header.addView(text("Gestión de ganadería ovina", 11, Color.WHITE).apply {
            setPadding(0, dp(12), 0, 0)
        })
        root.addView(header)
        scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = column().apply { setPadding(dp(22), dp(22), dp(22), dp(24)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav =
            row().apply { setPadding(dp(6), dp(8), dp(6), dp(8)); setBackgroundColor(Color.WHITE) }
        root.addView(
            View(this).apply { setBackgroundColor(line) },
            LinearLayout.LayoutParams(-1, dp(1))
        )
        root.addView(nav)
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar tu sesión?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        authScope.launch {
            try {
                SupabaseClientProvider.client.auth.signOut()

                val intent = Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    e.localizedMessage ?: "No se pudo cerrar la sesión.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun render() {
        content.removeAllViews()
        if (cargandoDatos) {
            title("AgroTech", "Cargando información...")
            return
        }

        if (!ranchoRegistrado) {
            registrarRanchoScreen()
            nav.visibility = View.GONE
            return
        }

        nav.visibility = View.VISIBLE
        when {
            route == "home" -> home()
            route == "settings" -> settings()
            route == "account_settings" -> accountSettings()
            route == "ranch_settings" -> ranchSettings()
            route == "add" -> addAnimal()
            route == "inventory" -> inventory()
            route == "metrics" -> metrics()
            route.startsWith("detail:") -> detail(route.substringAfter(':'))
            route.startsWith("weights:") -> weights(route.substringAfter(':'))
            else -> {
                route = "home"
                home()
            }
        }
        bottomNav()
        scroll.post { scroll.scrollTo(0, 0) }
    }

    private fun navigate(next: String, topLevel: Boolean = false) {
        hideKeyboard()
        captureDraft()
        if (topLevel) stack.clear() else if (next != route) stack += route
        route = next
        render()
    }

    private fun home() {
        title("Tu rancho, a la mano", "¿Qué quieres hacer hoy?")
        action("Agregar", "Animales y nuevos pesajes", "plus") { navigate("add", true) }
        action("Consultar", "Inventario y ficha de cada animal", "search") {
            navigate(
                "inventory",
                true
            )
        }
        action("Métricas", "Un vistazo al rebaño de ejemplo", "chart") { navigate("metrics", true) }
        note("Primera etapa: ficha individual e historial de pesos. Los cambios se conservan durante esta sesión de la app.")
    }

    private fun inventory() {
        title("Inventario", "${repo.all().size} animales de demostración")
        Category.entries.forEach { category ->
            section("${category.label} · ${repo.count(category)}")
            val animals = repo.all().filter { it.category == category }
            if (animals.isEmpty()) note("Todavía no hay animales en esta categoría.")
            animals.forEach { animal -> animalCard(animal) { navigate("detail:${animal.id}") } }
        }
        primary("Agregar animal") { navigate("add") }
    }

    private fun detail(id: String) {
        val animal = repo.find(id) ?: return missingAnimal()
        bottomBackButton()
        title("Arete ${animal.id}", "${animal.breed} · ${animal.sex.label}")
        badge(animal.stage)
        value("Nacimiento", displayDate.format(animal.birth))
        value("Edad", "${ChronoUnit.DAYS.between(animal.birth, LocalDate.now())} días")
        value("Categoría", animal.category.label)
        value("Corral", animal.pen)
        value("SINIIGA / chip", animal.chip.ifEmpty { "Sin registrar" })
        value(
            "Último peso",
            animal.weights.lastOrNull()?.let { "${number(it.kg)} kg" } ?: "Sin registro")
        section("Familia")
        var familyFound = false
        listOf(
            "Madre" to animal.motherId,
            "Padre" to animal.fatherId
        ).forEach { (label, parentId) ->
            repo.find(parentId ?: "")?.let { parent ->
                familyFound = true
                link("$label · Arete ${parent.id}") { navigate("detail:${parent.id}") }
            }
        }
        repo.all().filter { it.motherId == id || it.fatherId == id }.forEach { child ->
            familyFound = true
            link("Cría · Arete ${child.id}") { navigate("detail:${child.id}") }
        }
        if (!familyFound) note("Sin parentescos registrados.")
        primary("Ver y registrar pesos") { navigate("weights:$id") }
        note("Sanidad, alimentación y reproducción se incorporarán en etapas posteriores.")
    }

    private fun addAnimal() {
        title("Agregar animal", "Captura básica · Guardado de demostración")
        val arete =
            field("Arete interno *", "Ej. 102", R.id.animal_arete, draft.getString("id", ""), 30)
        label("Sexo")
        val sex = picker(Sex.entries.map { it.label }, R.id.animal_sex, draft.getInt("sex"))
        label("Categoría")
        val category = picker(
            Category.entries.map { it.label },
            R.id.animal_category,
            draft.getInt("category")
        )
        val breed =
            field("Raza *", "Ej. Dorper", R.id.animal_breed, draft.getString("breed", "Dorper"), 50)
        label("Fecha de nacimiento *")
        val dateButton = secondary(birth?.format(displayDate) ?: "Seleccionar fecha") {}
        dateButton.setOnClickListener {
            chooseDate(birth ?: LocalDate.now(), null) { selected ->
                birth = selected
                dateButton.text = displayDate.format(selected)
            }
        }
        val chip = field(
            "SINIIGA / microchip (opcional)",
            "Si ya cuenta con él",
            R.id.animal_chip,
            draft.getString("chip", ""),
            30
        )
        val error = errorText()
        primary("Agregar a la demostración") {
            val date = birth
            if (date == null) {
                showError(error, "Selecciona la fecha de nacimiento."); return@primary
            }
            try {
                val animal = repo.add(
                    arete.text.toString(),
                    Sex.entries[sex.selectedItemPosition],
                    Category.entries[category.selectedItemPosition],
                    breed.text.toString(),
                    chip.text.toString(),
                    date
                )
                hideKeyboard()
                draft = Bundle(); birth = null
                stack.clear(); stack += "inventory"
                route = "detail:${animal.id}"
                render()
                Toast.makeText(this, "Animal agregado a la demostración", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                showError(error, e.message ?: "Revisa los datos.")
            }
        }
        note("Los datos son de prueba. El registro formal de nacimientos y su evidencia tendrán un flujo propio.")
    }

    private fun weights(id: String) {
        val animal = repo.find(id) ?: return missingAnimal()
        bottomBackButton()
        title("Pesos · ${animal.id}", "Historial del animal en kilogramos")
        if (animal.weights.isEmpty()) note("Aún no hay pesajes.")
        animal.weights.forEach { weight ->
            value(
                displayDate.format(weight.date),
                "${number(weight.kg)} kg"
            )
        }
        section("Nuevo pesaje")
        label("Fecha *")
        val dateButton = secondary(displayDate.format(weightDate)) {}
        dateButton.setOnClickListener {
            chooseDate(weightDate.coerceAtLeast(animal.birth), animal.birth) { selected ->
                weightDate = selected; dateButton.text = displayDate.format(selected)
            }
        }
        val kg = field(
            "Peso (kg) *",
            "Ej. 12.5",
            R.id.weight_amount,
            draft.getString("weight:$id", ""),
            10
        ).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val error = errorText()
        primary("Guardar pesaje de ejemplo") {
            val amount = kg.text.toString().trim().replace(',', '.').toDoubleOrNull()
            if (amount == null) {
                showError(error, "Escribe un peso válido en kilogramos."); return@primary
            }
            try {
                repo.addWeight(id, weightDate, amount)
                draft.remove("weight:$id")
                hideKeyboard()
                render()
                Toast.makeText(this, "Pesaje agregado a la demostración", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                showError(error, e.message ?: "Revisa el pesaje.")
            }
        }
        note("Un pesaje por fecha en esta maqueta. Los registros se ordenan por fecha; no se reemplaza el historial.")
    }

    private fun metrics() {
        title("El rebaño en números", "Calculado únicamente con la demostración")
        val stats = row().apply { setPadding(0, dp(16), 0, dp(8)) }
        listOf(
            repo.all().size to "Animales registrados",
            repo.count(Category.LAMBS) to "Crías registradas"
        ).forEachIndexed { index, (count, caption) ->
            val box = column().apply {
                background = rounded(Color.WHITE, 16, line)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                addView(text(count.toString(), 30, forest, true))
                addView(text(caption, 12, muted))
            }
            stats.addView(
                box,
                LinearLayout.LayoutParams(0, -1, 1f).apply { if (index == 0) marginEnd = dp(12) })
        }
        content.addView(stats)
        section("Inventario por categoría")
        Category.entries.forEach { value(it.label, repo.count(it).toString()) }
        note("Preñez, mortalidad y costos se mostrarán cuando existan eventos y periodos suficientes. Los supuestos del Excel no son registros reales del rancho.")
    }

    private fun registrarRanchoScreen() {
        title("Registra tu rancho", "Completa los datos para comenzar a utilizar AgroTech.")

        val nombre = field(
            "Nombre del rancho *",
            "Ej. Rancho Los Olivos",
            View.generateViewId(),
            "",
            80
        )

        val localidad = field(
            "Localidad",
            "Ej. San Miguel",
            View.generateViewId(),
            "",
            80
        )

        val municipio = field(
            "Municipio",
            "Ej. Querétaro",
            View.generateViewId(),
            "",
            80
        )

        val estado = field(
            "Estado o región",
            "Ej. Querétaro",
            View.generateViewId(),
            "",
            80
        )

        val error = errorText()

        primary("Registrar mi rancho") {
            val nombreTexto = nombre.text.toString().trim()
            val localidadTexto = localidad.text.toString().trim()
            val municipioTexto = municipio.text.toString().trim()
            val estadoTexto = estado.text.toString().trim()

            when {
                nombreTexto.isBlank() -> {
                    showError(error, "Escribe el nombre del rancho.")
                    return@primary
                }

                localidadTexto.isBlank() -> {
                    showError(error, "Escribe la localidad.")
                    return@primary
                }

                municipioTexto.isBlank() -> {
                    showError(error, "Escribe el municipio.")
                    return@primary
                }

                estadoTexto.isBlank() -> {
                    showError(error, "Escribe el estado o región.")
                    return@primary
                }
            }

            supabaseScope.launch {
                try {
                    SupabaseRepository().registrarRancho(
                        nombre = nombreTexto,
                        localidad = localidadTexto,
                        municipio = municipioTexto,
                        estadoRegion = estadoTexto
                    )

                    nombreRancho = nombreTexto
                    headerRancho.text = nombreRancho
                    ranchoRegistrado = true
                    route = "home"
                    render()

                    Toast.makeText(
                        this@MainActivity,
                        "¡Rancho registrado correctamente!",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (e: Exception) {
                    Log.e("AGROTECH_SUPABASE", "Error al registrar rancho", e)
                    showError(error, e.localizedMessage ?: "No se pudo registrar el rancho.")
                }
            }
        }
    }

    private fun settings() {
        title("Configuración", "Administra tu cuenta y tu rancho.")

        action(
            "Mi cuenta",
            "Cambiar contraseña",
            "user"
        ) {
            navigate("account_settings")
        }

        action(
            "Datos del rancho",
            "Editar información del rancho",
            "home"
        ) {
            navigate("ranch_settings")
        }

        section("Sesión")

        action(
            "Cerrar sesión",
            "Salir de tu cuenta de AgroTech",
            "logout"
        ) {
            confirmLogout()
        }
        bottomBackButton()
    }

    private fun accountSettings() {
        title("Mi cuenta", "Administra la seguridad de tu cuenta.")

        val correo = SupabaseClientProvider.client.auth
            .currentUserOrNull()?.email ?: "No disponible"

        section("Información de la cuenta")
        value("Correo electrónico", correo)

        note("El correo electrónico y el nombre de usuario no se pueden modificar.")

        section("Cambiar contraseña")

        val actual = field(
            "Contraseña actual *",
            "Escribe tu contraseña actual",
            View.generateViewId(),
            "",
            100
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }

        val nueva = field(
            "Nueva contraseña *",
            "Mínimo 8 caracteres",
            View.generateViewId(),
            "",
            100
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }

        val confirmar = field(
            "Confirmar nueva contraseña *",
            "Repite la nueva contraseña",
            View.generateViewId(),
            "",
            100
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }

        val error = errorText()

        primary("Actualizar contraseña") {
            val actualTexto = actual.text.toString()
            val nuevaTexto = nueva.text.toString()
            val confirmarTexto = confirmar.text.toString()

            when {
                actualTexto.isBlank() -> {
                    showError(error, "Escribe tu contraseña actual.")
                    return@primary
                }

                nuevaTexto.length < 8 -> {
                    showError(error, "La nueva contraseña debe tener al menos 8 caracteres.")
                    return@primary
                }

                nuevaTexto != confirmarTexto -> {
                    showError(error, "Las contraseñas nuevas no coinciden.")
                    return@primary
                }

                actualTexto == nuevaTexto -> {
                    showError(error, "La nueva contraseña debe ser diferente a la actual.")
                    return@primary
                }
            }

            hideKeyboard()

            supabaseScope.launch {
                try {
                    SupabaseRepository().cambiarContrasena(
                        contrasenaActual = actualTexto,
                        nuevaContrasena = nuevaTexto
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Contraseña actualizada correctamente.",
                        Toast.LENGTH_LONG
                    ).show()

                    navigate("settings", true)

                } catch (e: Exception) {
                    Log.e("AGROTECH_AUTH", "Error al cambiar contraseña", e)
                    showError(
                        error,
                        "No se pudo actualizar la contraseña. Verifica tu contraseña actual e inténtalo nuevamente."
                    )
                }
            }
        }
        bottomBackButton()
    }

    private fun ranchSettings() {
        title("Datos del rancho", "Consulta y modifica la información de tu rancho.")

        val nombre = field(
            "Nombre del rancho *",
            "Ej. Rancho Los Olivos",
            View.generateViewId(),
            "",
            80
        )

        val localidad = field(
            "Localidad *",
            "Ej. San Miguel",
            View.generateViewId(),
            "",
            80
        )

        val municipio = field(
            "Municipio *",
            "Ej. Querétaro",
            View.generateViewId(),
            "",
            80
        )

        val estado = field(
            "Estado o región *",
            "Ej. Querétaro",
            View.generateViewId(),
            "",
            80
        )

        val error = errorText()

        lateinit var guardar: Button
        guardar = primary("Guardar cambios") {
            val nombreTexto = nombre.text.toString().trim()
            val localidadTexto = localidad.text.toString().trim()
            val municipioTexto = municipio.text.toString().trim()
            val estadoTexto = estado.text.toString().trim()

            when {
                nombreTexto.isBlank() -> {
                    showError(error, "Escribe el nombre del rancho.")
                    return@primary
                }

                localidadTexto.isBlank() -> {
                    showError(error, "Escribe la localidad.")
                    return@primary
                }

                municipioTexto.isBlank() -> {
                    showError(error, "Escribe el municipio.")
                    return@primary
                }

                estadoTexto.isBlank() -> {
                    showError(error, "Escribe el estado o región.")
                    return@primary
                }
            }

            hideKeyboard()
            guardar.isEnabled = false
            guardar.text = "Guardando..."

            supabaseScope.launch {
                try {
                    SupabaseRepository().actualizarRancho(
                        nombre = nombreTexto,
                        localidad = localidadTexto,
                        municipio = municipioTexto,
                        estadoRegion = estadoTexto
                    )

                    nombreRancho = nombreTexto
                    headerRancho.text = nombreTexto

                    Toast.makeText(
                        this@MainActivity,
                        "Datos del rancho actualizados correctamente.",
                        Toast.LENGTH_LONG
                    ).show()

                    navigate("settings", true)

                } catch (e: Exception) {
                    Log.e("AGROTECH_RANCHO", "Error al actualizar rancho", e)

                    showError(
                        error,
                        e.localizedMessage ?: "No se pudieron guardar los cambios."
                    )

                    guardar.isEnabled = true
                    guardar.text = "Guardar cambios"
                }
            }
        }

        guardar.isEnabled = false
        guardar.text = "Cargando datos..."

        supabaseScope.launch {
            try {
                val datos = SupabaseRepository().obtenerDatosUsuario()
                val rancho = datos?.rancho
                    ?: throw Exception("No se encontró el rancho de esta cuenta.")

                nombre.setText(rancho.nombre)
                localidad.setText(rancho.localidad.orEmpty())
                municipio.setText(rancho.municipio.orEmpty())
                estado.setText(rancho.estado_region.orEmpty())

                guardar.isEnabled = true
                guardar.text = "Guardar cambios"

            } catch (e: Exception) {
                Log.e("AGROTECH_RANCHO", "Error al cargar rancho", e)

                showError(
                    error,
                    e.localizedMessage ?: "No se pudieron cargar los datos del rancho."
                )

                guardar.isEnabled = false
                guardar.text = "No se pudieron cargar los datos"
            }
        }
        bottomBackButton()
    }
    private fun bottomNav() {
        nav.removeAllViews()
        val active = when {
            route.startsWith("detail:") || route.startsWith("weights:") -> "inventory"; else -> route
        }
        listOf(
            Triple("home", "Inicio", "home"), Triple("add", "Agregar", "plus"),
            Triple("inventory", "Consultar", "search"), Triple("metrics", "Métricas", "chart")
        ).forEach { (destination, caption, icon) ->
            val selected = active == destination
            val item = column().apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(58)
                background = rounded(if (selected) leaf else Color.WHITE, 14)
                isClickable = true; isFocusable = true; isSelected = selected
                contentDescription = caption
                addView(
                    LineIcon(this@MainActivity, icon, if (selected) forest else muted),
                    LinearLayout.LayoutParams(dp(23), dp(23))
                )
                addView(
                    text(
                        caption,
                        11,
                        if (selected) forest else muted,
                        selected
                    ).apply { setPadding(0, dp(4), 0, 0) })
                setOnClickListener { navigate(destination, true) }
            }
            nav.addView(
                item,
                LinearLayout.LayoutParams(0, -2, 1f)
                    .apply { marginStart = dp(2); marginEnd = dp(2) })
        }
    }

    private fun captureDraft() {
        if (route == "add") {
            draft.putString("id", findViewById<EditText>(R.id.animal_arete)?.text?.toString() ?: "")
            draft.putInt("sex", findViewById<Spinner>(R.id.animal_sex)?.selectedItemPosition ?: 0)
            draft.putInt(
                "category",
                findViewById<Spinner>(R.id.animal_category)?.selectedItemPosition ?: 0
            )
            draft.putString(
                "breed",
                findViewById<EditText>(R.id.animal_breed)?.text?.toString() ?: "Dorper"
            )
            draft.putString(
                "chip",
                findViewById<EditText>(R.id.animal_chip)?.text?.toString() ?: ""
            )
        } else if (route.startsWith("weights:")) {
            draft.putString(
                "weight:${route.substringAfter(':')}",
                findViewById<EditText>(R.id.weight_amount)?.text?.toString() ?: ""
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        captureDraft()
        outState.putString("route", route)
        outState.putStringArrayList("stack", ArrayList(stack))
        outState.putBundle("draft", draft)
        outState.putString("birth", birth?.toString())
        outState.putString("weightDate", weightDate.toString())
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Compatibilidad con Android 8 a 12")
    override fun onBackPressed() {
        back()
    }

    private fun back() {
        captureDraft(); hideKeyboard()
        if (stack.isNotEmpty()) {
            route = stack.removeAt(stack.lastIndex); render()
        } else if (route != "home") {
            route = "home"; render()
        } else finish()
    }

    private fun missingAnimal() {
        bottomBackButton(); title("Animal no disponible", "La sesión de demostración se reinició.")
        primary("Volver al inventario") { navigate("inventory", true) }
    }

    private fun chooseDate(initial: LocalDate, minimum: LocalDate?, selected: (LocalDate) -> Unit) {
        DatePickerDialog(
            this,
            { _, y, m, d -> selected(LocalDate.of(y, m + 1, d)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            minimum?.let {
                datePicker.minDate =
                    it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            show()
        }
    }

    private fun title(heading: String, subtitle: String) {
        content.addView(text(heading, 24, ink, true))
        content.addView(text(subtitle, 13, muted).apply { setPadding(0, dp(4), 0, dp(12)) })
    }

    private fun section(caption: String) {
        content.addView(text(caption, 16, ink, true).apply { setPadding(0, dp(18), 0, dp(8)) })
    }

    private fun note(caption: String) {
        content.addView(text(caption, 12, muted).apply { setPadding(0, dp(16), 0, dp(4)) })
    }

    private fun label(caption: String) {
        content.addView(text(caption, 13, muted).apply { setPadding(0, dp(14), 0, dp(6)) })
    }

    private fun badge(caption: String) {
        content.addView(text(caption, 12, forest, true).apply {
            background = rounded(leaf, 8); setPadding(dp(10), dp(5), dp(10), dp(5))
        }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(10) })
    }

    private fun value(caption: String, value: String) {
        val box =
            row().apply { setPadding(0, dp(13), 0, dp(13)); gravity = Gravity.CENTER_VERTICAL }
        box.addView(text(caption, 14, muted), LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(
            text(value, 14, ink, true).apply { gravity = Gravity.END },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        content.addView(box)
        content.addView(
            View(this).apply { setBackgroundColor(line) },
            LinearLayout.LayoutParams(-1, dp(1))
        )
    }

    private fun action(caption: String, subtitle: String, icon: String, click: () -> Unit) {
        val box = row().apply {
            gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(96)
            setPadding(dp(16), dp(18), dp(16), dp(18)); background = rounded(Color.WHITE, 18, line)
            elevation = dp(1).toFloat(); isClickable = true; isFocusable = true
            contentDescription = "$caption. $subtitle"; setOnClickListener { click() }
        }
        val iconView = LineIcon(this, icon, forest).apply {
            background = rounded(leaf, 13); setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )
        }
        box.addView(iconView, LinearLayout.LayoutParams(dp(50), dp(50)))
        val words = column().apply {
            setPadding(dp(14), 0, dp(8), 0)
            addView(text(caption, 20, ink, true))
            addView(text(subtitle, 12, muted))
        }
        box.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(LineIcon(this, "arrow", forest), LinearLayout.LayoutParams(dp(18), dp(24)))
        content.addView(
            box,
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
    }

    private fun animalCard(animal: Animal, click: () -> Unit) {
        link("Arete ${animal.id}\n${animal.breed} · ${animal.sex.label} · ${animal.stage}", click)
    }

    private fun link(caption: String, click: () -> Unit) {
        secondary("$caption  ›", click)
    }

    private fun bottomBackButton() {
        val button = Button(this).apply {
            text = "Volver"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.rgb(180, 25, 25))
            setTypeface(typeface, Typeface.BOLD)

            background = rounded(
                Color.rgb(255, 220, 220),
                10
            )

            minHeight = dp(46)
            setPadding(dp(28), dp(8), dp(28), dp(8))

            setOnClickListener {
                back()
            }
        }

        content.addView(
            button,
            LinearLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(28)
                bottomMargin = dp(12)
            }
        )
    }

    private fun primary(caption: String, click: () -> Unit) =
        button(caption, forest, Color.WHITE, click)

    private fun secondary(caption: String, click: () -> Unit) =
        button(caption, Color.WHITE, ink, click, true)

    private fun button(
        caption: String,
        fill: Int,
        color: Int,
        click: () -> Unit,
        outlined: Boolean = false
    ): Button {
        val button = Button(this).apply {
            text = caption; isAllCaps = false; textSize = 15f; setTextColor(color)
            setTypeface(typeface, if (outlined) Typeface.NORMAL else Typeface.BOLD)
            background = rounded(fill, 12, if (outlined) line else null)
            stateListAnimator = null; minHeight = dp(50)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            gravity = if (outlined) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
            setOnClickListener { click() }
        }
        content.addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        return button
    }

    private fun field(
        caption: String,
        hint: String,
        viewId: Int,
        initial: String?,
        max: Int
    ): EditText {
        label(caption)
        val field = EditText(this).apply {
            id = viewId; this.hint = hint; textSize =
            16f; setTextColor(ink); setHintTextColor(muted)
            setSingleLine(true); inputType = InputType.TYPE_CLASS_TEXT
            filters = arrayOf(InputFilter.LengthFilter(max)); setText(initial ?: "")
            background = rounded(Color.WHITE, 10, line); setPadding(dp(14), dp(12), dp(14), dp(12))
            minHeight = dp(50)
        }
        content.addView(field, LinearLayout.LayoutParams(-1, -2))
        return field
    }

    private fun picker(options: List<String>, viewId: Int, selected: Int): Spinner {
        val spinner = Spinner(this).apply {
            id = viewId; adapter =
            ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options)
            setSelection(selected.coerceIn(options.indices)); backgroundTintList =
            ColorStateList.valueOf(forest)
            minimumHeight = dp(50)
        }
        content.addView(spinner, LinearLayout.LayoutParams(-1, -2))
        return spinner
    }

    private fun errorText() = text("", 13, Color.rgb(160, 38, 38)).also {
        it.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        content.addView(it)
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        scroll.post { scroll.smoothScrollTo(0, view.top) }
    }

    private fun text(caption: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = caption; textSize = size.toFloat(); setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    private fun rounded(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(radius).toFloat(); stroke?.let { setStroke(dp(1), it) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()
    private fun number(value: Double) = String.format(Locale.forLanguageTag("es-MX"), "%.1f", value)
    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            root.windowToken,
            0
        ); root.clearFocus()
    }

    override fun onDestroy() {
        authScope.cancel()
        supabaseScope.cancel()
        super.onDestroy()
    }

    private fun probarConsultaSupabase() {
        supabaseScope.launch {
            try {
                val datos = SupabaseRepository().obtenerDatosUsuario()

                if (datos == null) {
                    cargandoDatos = false
                    Log.w("AGROTECH_SUPABASE", "No se encontró el perfil o no hay sesión.")
                    Toast.makeText(
                        this@MainActivity,
                        "No se encontraron los datos del usuario. Verifica tu perfil.",
                        Toast.LENGTH_LONG
                    ).show()
                    render()
                    return@launch
                }

                ranchoRegistrado = datos.rancho != null
                nombreRancho = datos.rancho?.nombre ?: ""
                cargandoDatos = false
                headerRancho.text = nombreRancho.ifBlank { "AgroTech" }

                Log.d("AGROTECH_SUPABASE", "Perfil: ${datos.perfil}")
                Log.d("AGROTECH_SUPABASE", "Rancho: ${datos.rancho}")

                render()

            } catch (e: Exception) {
                cargandoDatos = false
                Log.e("AGROTECH_SUPABASE", "Error al consultar", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error al consultar Supabase: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                render()
            }
        }
    }
}