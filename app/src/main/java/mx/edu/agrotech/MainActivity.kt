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

/** Interfaz nativa Android en Kotlin. Los módulos de datos no dependen de esta Activity. */
class MainActivity : Activity() {
    private val repo get() = (application as DemoApplication).repository
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
    private val stack = mutableListOf<String>()
    private var route = "home"
    private var birth: LocalDate? = null
    private var weightDate = LocalDate.now()
    private val displayDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX"))
    private var draft = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route = savedInstanceState?.getString("route") ?: "home"
        stack.addAll(savedInstanceState?.getStringArrayList("stack") ?: emptyList())
        draft = savedInstanceState?.getBundle("draft") ?: Bundle()
        birth = savedInstanceState?.getString("birth")?.let(LocalDate::parse)
        weightDate = savedInstanceState?.getString("weightDate")?.let(LocalDate::parse) ?: LocalDate.now()
        buildShell()
        render()
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(0) { back() }
        }
    }

    private fun buildShell() {
        root = column().apply { setBackgroundColor(paper) }
        setContentView(root)
        // Aplica barras del sistema y teclado en Android moderno, incluido edge-to-edge.
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val system = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                view.setPadding(system.left, system.top, system.right, maxOf(system.bottom, keyboard.bottom))
                nav.visibility = if (insets.isVisible(WindowInsets.Type.ime())) View.GONE else View.VISIBLE
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        val header = column().apply { setPadding(dp(24), dp(18), dp(24), dp(19)); setBackgroundColor(forest) }
        val brand = row()
        brand.addView(LineIcon(this, "sprout", Color.WHITE), LinearLayout.LayoutParams(dp(22), dp(22)))
        brand.addView(text("AGROTECH", 12, Color.WHITE, true).apply { letterSpacing = .16f; setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        brand.addView(text("MAQUETA", 10, leaf, true))
        header.addView(brand)
        header.addView(text("Rancho El Encino", 25, Color.WHITE, true).apply { setPadding(0, dp(12), 0, 0) })
        header.addView(text("Ganadería ovina", 14, leaf))
        header.addView(text("Datos ficticios · Sin bases de datos", 11, Color.WHITE).apply { setPadding(0, dp(12), 0, 0) })
        root.addView(header)
        scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = column().apply { setPadding(dp(22), dp(22), dp(22), dp(24)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = row().apply { setPadding(dp(6), dp(8), dp(6), dp(8)); setBackgroundColor(Color.WHITE) }
        root.addView(View(this).apply { setBackgroundColor(line) }, LinearLayout.LayoutParams(-1, dp(1)))
        root.addView(nav)
    }

    private fun render() {
        content.removeAllViews()
        when {
            route == "home" -> home()
            route == "add" -> addAnimal()
            route == "inventory" -> inventory()
            route == "metrics" -> metrics()
            route.startsWith("detail:") -> detail(route.substringAfter(':'))
            route.startsWith("weights:") -> weights(route.substringAfter(':'))
            else -> { route = "home"; home() }
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
        action("Consultar", "Inventario y ficha de cada animal", "search") { navigate("inventory", true) }
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
        backButton()
        title("Arete ${animal.id}", "${animal.breed} · ${animal.sex.label}")
        badge(animal.stage)
        value("Nacimiento", displayDate.format(animal.birth))
        value("Edad", "${ChronoUnit.DAYS.between(animal.birth, LocalDate.now())} días")
        value("Categoría", animal.category.label)
        value("Corral", animal.pen)
        value("SINIIGA / chip", animal.chip.ifEmpty { "Sin registrar" })
        value("Último peso", animal.weights.lastOrNull()?.let { "${number(it.kg)} kg" } ?: "Sin registro")
        section("Familia")
        var familyFound = false
        listOf("Madre" to animal.motherId, "Padre" to animal.fatherId).forEach { (label, parentId) ->
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
        val arete = field("Arete interno *", "Ej. 102", R.id.animal_arete, draft.getString("id", ""), 30)
        label("Sexo")
        val sex = picker(Sex.entries.map { it.label }, R.id.animal_sex, draft.getInt("sex"))
        label("Categoría")
        val category = picker(Category.entries.map { it.label }, R.id.animal_category, draft.getInt("category"))
        val breed = field("Raza *", "Ej. Dorper", R.id.animal_breed, draft.getString("breed", "Dorper"), 50)
        label("Fecha de nacimiento *")
        val dateButton = secondary(birth?.format(displayDate) ?: "Seleccionar fecha") {}
        dateButton.setOnClickListener {
            chooseDate(birth ?: LocalDate.now(), null) { selected ->
                birth = selected
                dateButton.text = displayDate.format(selected)
            }
        }
        val chip = field("SINIIGA / microchip (opcional)", "Si ya cuenta con él", R.id.animal_chip, draft.getString("chip", ""), 30)
        val error = errorText()
        primary("Agregar a la demostración") {
            val date = birth
            if (date == null) { showError(error, "Selecciona la fecha de nacimiento."); return@primary }
            try {
                val animal = repo.add(arete.text.toString(), Sex.entries[sex.selectedItemPosition],
                    Category.entries[category.selectedItemPosition], breed.text.toString(), chip.text.toString(), date)
                hideKeyboard()
                draft = Bundle(); birth = null
                stack.clear(); stack += "inventory"
                route = "detail:${animal.id}"
                render()
                Toast.makeText(this, "Animal agregado a la demostración", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) { showError(error, e.message ?: "Revisa los datos.") }
        }
        note("Los datos son de prueba. El registro formal de nacimientos y su evidencia tendrán un flujo propio.")
    }

    private fun weights(id: String) {
        val animal = repo.find(id) ?: return missingAnimal()
        backButton()
        title("Pesos · ${animal.id}", "Historial del animal en kilogramos")
        if (animal.weights.isEmpty()) note("Aún no hay pesajes.")
        animal.weights.forEach { weight -> value(displayDate.format(weight.date), "${number(weight.kg)} kg") }
        section("Nuevo pesaje")
        label("Fecha *")
        val dateButton = secondary(displayDate.format(weightDate)) {}
        dateButton.setOnClickListener {
            chooseDate(weightDate.coerceAtLeast(animal.birth), animal.birth) { selected ->
                weightDate = selected; dateButton.text = displayDate.format(selected)
            }
        }
        val kg = field("Peso (kg) *", "Ej. 12.5", R.id.weight_amount, draft.getString("weight:$id", ""), 10).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val error = errorText()
        primary("Guardar pesaje de ejemplo") {
            val amount = kg.text.toString().trim().replace(',', '.').toDoubleOrNull()
            if (amount == null) { showError(error, "Escribe un peso válido en kilogramos."); return@primary }
            try {
                repo.addWeight(id, weightDate, amount)
                draft.remove("weight:$id")
                hideKeyboard()
                render()
                Toast.makeText(this, "Pesaje agregado a la demostración", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) { showError(error, e.message ?: "Revisa el pesaje.") }
        }
        note("Un pesaje por fecha en esta maqueta. Los registros se ordenan por fecha; no se reemplaza el historial.")
    }

    private fun metrics() {
        title("El rebaño en números", "Calculado únicamente con la demostración")
        val stats = row().apply { setPadding(0, dp(16), 0, dp(8)) }
        listOf(repo.all().size to "Animales registrados", repo.count(Category.LAMBS) to "Crías registradas").forEachIndexed { index, (count, caption) ->
            val box = column().apply {
                background = rounded(Color.WHITE, 16, line)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                addView(text(count.toString(), 30, forest, true))
                addView(text(caption, 12, muted))
            }
            stats.addView(box, LinearLayout.LayoutParams(0, -1, 1f).apply { if (index == 0) marginEnd = dp(12) })
        }
        content.addView(stats)
        section("Inventario por categoría")
        Category.entries.forEach { value(it.label, repo.count(it).toString()) }
        note("Preñez, mortalidad y costos se mostrarán cuando existan eventos y periodos suficientes. Los supuestos del Excel no son registros reales del rancho.")
    }

    private fun bottomNav() {
        nav.removeAllViews()
        val active = when { route.startsWith("detail:") || route.startsWith("weights:") -> "inventory"; else -> route }
        listOf(Triple("home", "Inicio", "home"), Triple("add", "Agregar", "plus"),
            Triple("inventory", "Consultar", "search"), Triple("metrics", "Métricas", "chart")).forEach { (destination, caption, icon) ->
            val selected = active == destination
            val item = column().apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(58)
                background = rounded(if (selected) leaf else Color.WHITE, 14)
                isClickable = true; isFocusable = true; isSelected = selected
                contentDescription = caption
                addView(LineIcon(this@MainActivity, icon, if (selected) forest else muted), LinearLayout.LayoutParams(dp(23), dp(23)))
                addView(text(caption, 11, if (selected) forest else muted, selected).apply { setPadding(0, dp(4), 0, 0) })
                setOnClickListener { navigate(destination, true) }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
        }
    }

    private fun captureDraft() {
        if (route == "add") {
            draft.putString("id", findViewById<EditText>(R.id.animal_arete)?.text?.toString() ?: "")
            draft.putInt("sex", findViewById<Spinner>(R.id.animal_sex)?.selectedItemPosition ?: 0)
            draft.putInt("category", findViewById<Spinner>(R.id.animal_category)?.selectedItemPosition ?: 0)
            draft.putString("breed", findViewById<EditText>(R.id.animal_breed)?.text?.toString() ?: "Dorper")
            draft.putString("chip", findViewById<EditText>(R.id.animal_chip)?.text?.toString() ?: "")
        } else if (route.startsWith("weights:")) {
            draft.putString("weight:${route.substringAfter(':')}", findViewById<EditText>(R.id.weight_amount)?.text?.toString() ?: "")
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
    override fun onBackPressed() { back() }

    private fun back() {
        captureDraft(); hideKeyboard()
        if (stack.isNotEmpty()) { route = stack.removeAt(stack.lastIndex); render() }
        else if (route != "home") { route = "home"; render() }
        else finish()
    }

    private fun missingAnimal() {
        backButton(); title("Animal no disponible", "La sesión de demostración se reinició.")
        primary("Volver al inventario") { navigate("inventory", true) }
    }
    private fun chooseDate(initial: LocalDate, minimum: LocalDate?, selected: (LocalDate) -> Unit) {
        DatePickerDialog(this, { _, y, m, d -> selected(LocalDate.of(y, m + 1, d)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
            minimum?.let { datePicker.minDate = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
            show()
        }
    }
    private fun title(heading: String, subtitle: String) {
        content.addView(text(heading, 24, ink, true))
        content.addView(text(subtitle, 13, muted).apply { setPadding(0, dp(4), 0, dp(12)) })
    }
    private fun section(caption: String) { content.addView(text(caption, 16, ink, true).apply { setPadding(0, dp(18), 0, dp(8)) }) }
    private fun note(caption: String) { content.addView(text(caption, 12, muted).apply { setPadding(0, dp(16), 0, dp(4)) }) }
    private fun label(caption: String) { content.addView(text(caption, 13, muted).apply { setPadding(0, dp(14), 0, dp(6)) }) }
    private fun badge(caption: String) {
        content.addView(text(caption, 12, forest, true).apply {
            background = rounded(leaf, 8); setPadding(dp(10), dp(5), dp(10), dp(5))
        }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(10) })
    }
    private fun value(caption: String, value: String) {
        val box = row().apply { setPadding(0, dp(13), 0, dp(13)); gravity = Gravity.CENTER_VERTICAL }
        box.addView(text(caption, 14, muted), LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(text(value, 14, ink, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(box)
        content.addView(View(this).apply { setBackgroundColor(line) }, LinearLayout.LayoutParams(-1, dp(1)))
    }
    private fun action(caption: String, subtitle: String, icon: String, click: () -> Unit) {
        val box = row().apply {
            gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(96)
            setPadding(dp(16), dp(18), dp(16), dp(18)); background = rounded(Color.WHITE, 18, line)
            elevation = dp(1).toFloat(); isClickable = true; isFocusable = true
            contentDescription = "$caption. $subtitle"; setOnClickListener { click() }
        }
        val iconView = LineIcon(this, icon, forest).apply { background = rounded(leaf, 13); setPadding(dp(12), dp(12), dp(12), dp(12)) }
        box.addView(iconView, LinearLayout.LayoutParams(dp(50), dp(50)))
        val words = column().apply {
            setPadding(dp(14), 0, dp(8), 0)
            addView(text(caption, 20, ink, true))
            addView(text(subtitle, 12, muted))
        }
        box.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(LineIcon(this, "arrow", forest), LinearLayout.LayoutParams(dp(18), dp(24)))
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
    }
    private fun animalCard(animal: Animal, click: () -> Unit) {
        link("Arete ${animal.id}\n${animal.breed} · ${animal.sex.label} · ${animal.stage}", click)
    }
    private fun link(caption: String, click: () -> Unit) { secondary("$caption  ›", click) }
    private fun backButton() {
        content.addView(Button(this).apply {
            text = "← Volver"; isAllCaps = false; textSize = 14f; setTextColor(forest)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL; background = rounded(paper, 8)
            setPadding(0, 0, dp(16), 0); minHeight = dp(48)
            setOnClickListener { back() }
        }, LinearLayout.LayoutParams(-2, -2))
    }
    private fun primary(caption: String, click: () -> Unit) = button(caption, forest, Color.WHITE, click)
    private fun secondary(caption: String, click: () -> Unit) = button(caption, Color.WHITE, ink, click, true)
    private fun button(caption: String, fill: Int, color: Int, click: () -> Unit, outlined: Boolean = false): Button {
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
    private fun field(caption: String, hint: String, viewId: Int, initial: String?, max: Int): EditText {
        label(caption)
        val field = EditText(this).apply {
            id = viewId; this.hint = hint; textSize = 16f; setTextColor(ink); setHintTextColor(muted)
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
            id = viewId; adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options)
            setSelection(selected.coerceIn(options.indices)); backgroundTintList = ColorStateList.valueOf(forest)
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
    private fun text(caption: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
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
    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0); root.clearFocus() }
}
