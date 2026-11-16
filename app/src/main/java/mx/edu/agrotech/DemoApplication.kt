package mx.edu.agrotech

import android.app.Application
import mx.edu.agrotech.data.DemoRepository

/** Almacén de demostración en memoria; sobrevive a la rotación de pantalla. */
class DemoApplication : Application() {
    val repository = DemoRepository()
}
