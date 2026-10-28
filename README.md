# Denuncias Lara - aplicación Android

Aplicación Java nativa con XML, Material Components, RecyclerView y Retrofit. Consume los cinco endpoints de `denunciaservice`; no se conecta directamente a la base de datos.

## Abrir y ejecutar paso a paso

1. En Android Studio elige **Open** y selecciona esta carpeta `denunciasapp` (la que contiene `settings.gradle`).
2. Espera la sincronización de Gradle. El proyecto utiliza Android Gradle Plugin **9.1.0**, Gradle **9.3.1**, `compileSdk 36`, `targetSdk 35` y `minSdk 26`.
3. En **Settings > Build, Execution, Deployment > Build Tools > Gradle**, selecciona un JDK compatible para Gradle. En esta computadora puede usarse el JDK 25 integrado en `D:\Android Studio\jbr`. El código se compila con nivel de lenguaje Java 17.
4. En **Tools > SDK Manager**, comprueba que esté instalado Android SDK Platform 36. Si Android Studio solicita la ubicación, usa `D:\AndroidSDK` en esta computadora. La ruta local queda en `local.properties`, excluido del repositorio.
5. Usa la API de Render configurada; para desarrollo local puedes cambiar la URL como se indica abajo.
6. Abre un emulador con Android 8.0/API 26 o posterior en **Device Manager**, selecciónalo y pulsa **Run app**.

El comando `java` de esta computadora puede apuntar a Java 8. No lo uses para ejecutar Gradle sin seleccionar antes el JDK adecuado.

## Configurar la API

El proyecto ya tiene configurada en `gradle.properties` la URL pública de la API desplegada en Render:

```properties
API_BASE_URL=https://denunciaservice-lara.onrender.com/
```

Despliegue verificado el 28/10/2026: servicio Live, cinco endpoints correctos y colección Postman ejecutada con 8 peticiones y 15 aserciones sin fallos. Antes de cada demostración, comprueba que Render y su base sigan disponibles.

Para desarrollar con la API local desde el emulador, cambia la propiedad y después pulsa **Sync Project with Gradle Files**:

```properties
API_BASE_URL=http://10.0.2.2:18080/
```

La URL debe terminar en `/`. Usa la raíz del servicio, sin añadir `denuncias/`, porque Retrofit agrega cada ruta.

| Ejecución | URL base |
| --- | --- |
| Emulador Android y API en la computadora | `http://10.0.2.2:18080/` |
| Celular físico y API local, ambos en la misma red | `http://IP_DE_TU_PC:18080/` |
| API pública en Render | `https://denunciaservice-lara.onrender.com/` |

En un teléfono, `localhost` se refiere al propio teléfono. Usa la IP IPv4 de la computadora obtenida con `ipconfig` y permite el puerto 18080 en la red privada si el firewall bloquea la conexión.

También puedes sobrescribir la propiedad para una compilación:

```powershell
.\gradlew.bat assembleDebug -PAPI_BASE_URL=https://denunciaservice-lara.onrender.com/
```

Desde la carpeta `Moviles`, el helper `.\scripts\Compilar-Android.ps1` respeta la URL de `gradle.properties` cuando se ejecuta sin parámetros. Para una compilación local puedes usar `.\scripts\Compilar-Android.ps1 -ApiBaseUrl 'http://10.0.2.2:18080/'`.

El permiso `INTERNET` está en el manifiesto principal. Solo la variante **debug** permite HTTP local mediante `usesCleartextTraffic="true"`; la variante release exige HTTPS. La API pública no necesita contraseñas de PostgreSQL dentro de la app.

## Flujo de las pantallas

1. **Lista:** al abrir consulta `GET denuncias/?offset=0&limit=5`. Cada clic en **Cargar 5 siguientes** agrega los siguientes registros: offsets 5, 10, 15. Cuando una respuesta trae menos de cinco se desactiva el botón. Si hay exactamente cinco al final, una consulta adicional confirma que terminó la lista.
2. **Registro:** valida DNI de ocho dígitos, dirección y descripción. La fecha se inicia con la hora del dispositivo y se modifica tocando el campo. Envía JSON con fecha ISO local (`yyyy-MM-dd'T'HH:mm:ss`). El servidor genera `id` y establece `estado=1`.
3. **Detalle:** al tocar una fila consulta `GET denuncias/{id}`. **Marcar como atendida** pide confirmar la atención y ejecuta `PUT denuncias/{id}` sin cuerpo. Muestra el estado 0 y actualiza la lista al regresar.
4. **Búsqueda:** escribe ocho dígitos y pulsa **Buscar**. Consulta `GET denuncias/ciudadano/{dni}` y muestra **todas** sus denuncias. **Volver a todas las denuncias** restablece la lista paginada.

Después de un registro exitoso se abre directamente el detalle de la nueva denuncia, aunque su identificador no pertenezca a la primera página de la lista.

## Contrato JSON

Solicitud de registro:

```json
{
  "dni": "01234567",
  "fecha": "2026-10-28T10:30:00",
  "tipo": "ROBO",
  "direccion": "Avenida de ejemplo 123",
  "descripcion": "Descripción de un incidente ficticio para la demostración."
}
```

Las rutas de lista y búsqueda responden con arreglos JSON (`[]` si no hay coincidencias). Las rutas de registro, detalle y atención responden con un objeto que incluye estos campos más `id` y `estado`. `estado=1` significa pendiente y `estado=0` significa atendida. El DNI es texto para conservar sus ceros iniciales.

## Archivos que debes comprender para la presentación

- `network/DenunciaApi.java`: las cinco rutas y sus métodos HTTP.
- `network/ApiClient.java`: URL, Retrofit, Gson y tiempos de espera (30 s conexión, 120 s lectura, 150 s total).
- `model/Denuncia.java` y `DenunciaRequest.java`: representación de la respuesta y del registro.
- `ui/MainActivity.java`: paginación y búsqueda por DNI.
- `ui/DenunciaAdapter.java`: filas de RecyclerView.
- `ui/RegisterActivity.java`: validación y POST.
- `ui/DetailActivity.java`: consulta individual y PUT de atención.
- `ui/BaseActivity.java`: llamadas asíncronas y márgenes de las barras del sistema; cancela las llamadas al destruir una pantalla.

## Comprobación manual antes de entregar

- Inicia la API y crea al menos **seis** denuncias ficticias para demostrar la segunda página.
- Comprueba cinco registros iniciales y el botón de los cinco siguientes, sin duplicados.
- Registra un DNI que empiece por cero y comprueba que conserva los ocho dígitos.
- Abre el detalle, atiende la denuncia y verifica el cambio a 0 tanto en la API como en la lista.
- Busca el DNI utilizado; si posee más de cinco denuncias, deben aparecer todas.
- Comprueba un DNI inválido y una consulta sin coincidencias.
- Desconecta la API y comprueba que aparece un mensaje y **Reintentar**.
- Prueba la URL de Render en el emulador y comprueba el permiso de Internet.

Se evita el doble clic durante cada petición. Las llamadas no se repiten automáticamente: si se pierde la respuesta de un registro, busca el DNI antes de volver a enviarlo para evitar duplicados. La primera conexión a Render puede tardar alrededor de un minuto.

No subas `local.properties`, `.gradle/`, `build/`, credenciales ni claves de firma. Debes crear y subir este proyecto a un repositorio remoto separado del repositorio de la API.
