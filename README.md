# Alma Quinta Growth Hub - Documentacion tecnica completa

## Descripcion general

`Alma Quinta Growth Hub` es una aplicacion Android nativa (Java) orientada al equipo de Growth de Alma Quinta para:

- Autenticar usuarios internos contra Firebase Authentication.
- Bloquear acceso cuando el usuario existe pero esta marcado como inactivo (`active=false`) en `Usuarios/{uid}`.
- Gestionar sesion y permisos por roles (`ADMIN`, `COORDINATOR`, `EMPLOYEE`).
- Leer metricas operativas almacenadas en Firebase Realtime Database.
- Mostrar un dashboard ejecutivo con indicadores agregados de trafico y comportamiento.
- Exponer vistas de analisis por rango temporal para:
  - Activos y nuevos usuarios.
  - Vistas por mes.
- Permitir exportacion CSV de analitica filtrada.
- Administrar usuarios internos (solo administradores): cambio de rol y activacion/desactivacion.
- Gestionar perfil de usuario autenticado (datos personales + foto en Base64).

La app no usa arquitectura MVVM ni Clean Architecture completa, pero si tiene separacion por capas (IU, data, domain, session, security) y un repositorio para analitica (`AnalyticsRepositoryImpl`) que desacopla el acceso a Firebase del render de pantallas.

---

## Tecnologias y dependencias

## Lenguaje y plataforma

- `Java 11` (configurado en `app/build.gradle`, `sourceCompatibility` y `targetCompatibility`).
- Android SDK:
  - `compileSdk 36`
  - `targetSdk 36`
  - `minSdk 24`
- Aplicacion Android con paquete: `com.almaquinta.analytics`.

## Build system y tooling

- `Gradle 9.4.1` (`gradle/wrapper/gradle-wrapper.properties`).
- Android Gradle Plugin `9.1.0` (`gradle/libs.versions.toml`).
- Plugin Google Services `com.google.gms.google-services` version `4.4.4` (`build.gradle` raiz).
- Catalogo de versiones con `libs.versions.toml`.
- Repositorios:
  - `google()`
  - `mavenCentral()`
  - `gradlePluginPortal()`

## Dependencias de app

Definidas en `app/build.gradle` y versionadas en `gradle/libs.versions.toml`:

- `androidx.appcompat:appcompat:1.7.1`
- `com.google.android.material:material:1.13.0`
- `androidx.activity:activity:1.13.0`
- `androidx.constraintlayout:constraintlayout:2.2.1`
- `com.airbnb.android:lottie:6.7.1`
- `com.google.firebase:firebase-analytics:23.2.0`
- `com.google.firebase:firebase-auth:24.0.1`
- `com.google.firebase:firebase-database:22.0.1`

## Dependencias de testing

- Unit test: `junit:junit:4.13.2`
- Instrumented test:
  - `androidx.test.ext:junit:1.3.0`
  - `androidx.test.espresso:espresso-core:3.7.0`

## Configuracion Gradle relevante

`gradle.properties`:

- `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`
- `android.useAndroidX=true`
- `android.nonTransitiveRClass=true`
- Flags de constraints de dependencias Android habilitados para performance de sync.

---

## Arquitectura y estructura del proyecto

## Vista global de carpetas

- `build.gradle`, `settings.gradle`, `gradle/`: configuracion de build.
- `app/build.gradle`: configuracion del modulo Android.
- `app/google-services.json`: configuracion Firebase del proyecto movil.
- `app/src/main/AndroidManifest.xml`: declaracion de Activities y `FileProvider`.
- `app/src/main/java/com/almaquinta/analytics/...`: codigo Java.
- `app/src/main/res/...`: layouts, strings, temas, drawables, xml de backup y file paths.
- `app/src/test/...` y `app/src/androidTest/...`: tests plantilla de Android Studio.

## Paquetes de codigo (app/src/main/java/com/almaquinta/analytics)

### `data/model`

- `AppUser`: entidad de usuario de app (id, nombre, apellido, email, rol, estado activo).
- `UserRole`: enum (`ADMIN`, `COORDINATOR`, `EMPLOYEE`).
- `AnalyticsSummary`: agregado mensual (anio, mes, visitas, sesiones, activos, nuevos, tasa, top source).
- `SourceMetric`: detalle por fuente (source, activeUsers, newUsers, engagementRate).

### `data/repository`

- `AnalyticsRepository`:
  - Contrato para observar data de dashboard por listener.
  - API de lectura puntual `getSummary(year, month)`.
- `AnalyticsRepositoryImpl`:
  - Implementacion concreta con Firebase Realtime Database (`estadisticas_sesiones`).
  - Agregacion de datasets (`vistas_x_mes` y `activosYNuevos`).
  - Cache en memoria de `AnalyticsSummary` por periodo.

### `domain`

- `CreateUserByAdminUseCase`:
  - Caso de uso simple para crear `AppUser` con validacion de acceso admin.
  - No esta conectado actualmente a un flujo real de UI/DB.

### `security`

- `AuthorizationService`:
  - Guardas de autorizacion por rol.
  - Metodos: `requireRegistrationAccess()`, `requireAdminOnly()`, `requireAdmin()`.

### `session`

- `SessionManager`:
  - Singleton en memoria para usuario actual.
  - Expone `isAdmin()` (ADMIN o COORDINATOR) y `isAdminOnly()` (solo ADMIN).

### `iu/common`

- `SystemBarsEdgeToEdge`:
  - Utilidad de configuracion Edge-to-Edge.
  - Controla insets de barras de sistema e IME para vistas objetivo.

### `iu/main`

- `SplashActivity`: ruteo inicial segun sesion Firebase + perfil en DB.
- `MainActivity`: login por correo/contrasena y carga de perfil.

### `iu/dashboard`

- `DashboardActivity`: pantalla principal con:
  - KPIs agregados.
  - desglose de fuentes.
  - grafica de barras (ProgressBar) de tendencia mensual.
  - menu lateral con acciones de navegacion.

### `iu/register`

- `RegisterActivity`: alta de usuarios por staff autorizado.

### `iu/activenew`

- `ActiveNewUsersActivity`: analitica de activos/nuevos por rango.

### `iu/viewspermonth`

- `ViewsPerMonthActivity`: analitica de vistas por rango.

### `iu/usermanagement`

- `UserManagementActivity`: gestion de usuarios (solo admin).

### `iu/profile`

- `ProfileActivity`: edicion de perfil y foto.

### `iu/about`

- `AboutAppActivity`: pantalla de informacion de app (ademas del dialog en dashboard).

## Patron de interaccion entre capas

- IU -> Repository (`AnalyticsRepository`) para lectura de metricas.
- IU -> Firebase Auth para autenticacion.
- IU -> Firebase Realtime Database para datos de usuario.
- IU -> SessionManager para estado de sesion local.
- IU -> AuthorizationService para control de acceso.

No hay inyeccion de dependencias ni framework de DI.

---

## Funcionalidades implementadas

## 1) Splash y ruteo de entrada

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/main/SplashActivity.java`

- Muestra `activity_splash.xml` con branding y animacion Lottie (`assets/loading.json`).
- Espera `3000 ms` usando `Handler` sobre `Looper` principal.
- Si no hay usuario Firebase autenticado -> redirige a `MainActivity` con `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
- Si hay sesion Firebase:
  - Lee `Usuarios/{uid}` en Realtime Database.
  - Si no existe snapshot -> `signOut()` y envio a login.
  - Si `active=false` -> cierra sesion (`FirebaseAuth` + `SessionManager`) y envia a login sin abrir dashboard.
  - Si existe:
    - Mapea `nombre`, `apellido`, `correo`, `role`, `active`.
    - Convierte `role` con `parseRole`.
    - Crea `AppUser` y actualiza `SessionManager`.
    - Abre `DashboardActivity`.

Detalle importante de implementacion:

- `parseRole()` usa `ADMIN` como fallback cuando `role` esta vacio/null.
- `getSafeBoolean()` retorna `true` por defecto cuando no puede parsear.

## 2) Login

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/main/MainActivity.java`

- UI de login (`activity_main.xml`) con email y password.
- Validaciones locales:
  - email con `Patterns.EMAIL_ADDRESS`.
  - password no vacia.
  - longitud minima de 8.
- Ejecuta `signInWithEmailAndPassword` con FirebaseAuth.
- Si autentica:
  - carga perfil en `Usuarios/{uid}`.
  - parsea campos de usuario.
  - si `active=false`, cancela el ingreso, cierra sesion y muestra el mensaje `Tu cuenta esta desactivada`.
  - si `role` esta vacio, persiste `role=ADMIN` en DB.
  - setea `SessionManager`.
  - navega a `DashboardActivity` y hace `finish()`.
- Manejo de errores con `Toast` + `ProgressDialog`.

## 3) Dashboard principal

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/dashboard/DashboardActivity.java`

### Navegacion y layout

- Estructura con `DrawerLayout` (`activity_dashboard.xml`).
- Menu lateral con:
  - registrar usuario (`btnRegister`)
  - perfil (`btnProfile`)
  - info de app (`btnAppInfo`)
  - cerrar sesion (`btnLogOut`)
- Acciones rapidas en cuerpo:
  - `Activos y Nuevos`
  - `Vistas por Mes`
  - `Administrar usuarios` (visible solo admin)

### Carga de datos de usuario

- Prioriza usuario de `SessionManager`.
- Si no existe en memoria, carga de `Usuarios/{uid}`.
- Muestra nombre y rol formateado.
- Carga avatar Base64 (`profileImageBase64`) en `ivDrawerAvatar`.
- Fallback de avatar: `R.drawable.company_lg`.

### Carga de analitica

- Se suscribe al repositorio (`observeDashboardData`).
- Guarda `allSummaries` y `sourcesByPeriod`.
- Construye filtros de anio/mes (spinners) una vez.
- Renderiza:
  - KPIs principales (visitas, sesiones, activos, nuevos, tasa, top fuente).
  - tendencia mensual con barras.
  - top 5 fuentes agregadas.

### Logica de agregacion en dashboard

- `filterSummaries()` actualmente toma los `3` meses mas recientes (`DASHBOARD_MONTH_LIMIT=3`), independientemente de los spinners.
- `aggregateSummaries()` suma visitas/sesiones/activos/nuevos.
- Tasa de interaccion agregada = promedio ponderado por usuarios activos.
- `aggregateSources()` consolida fuentes por periodos filtrados:
  - suma activos y nuevos
  - pondera tasa por activos
  - ordena descendente por activos

### Funciones adicionales

- Dialog de informacion (`showAboutDialog`) con `activity_about_app.xml`.
- Apertura de enlaces externos a GitHub y YouTube.
- Logout:
  - `FirebaseAuth.signOut()`
  - `SessionManager.logout()`
  - limpieza de stack y retorno a `MainActivity`.

## 4) Registro de usuarios internos

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/register/RegisterActivity.java`

### Control de acceso

- Al entrar, valida permisos con `AuthorizationService.requireRegistrationAccess()`.
- Si no autorizado:
  - muestra mensaje
  - redirige a dashboard o login segun sesion
  - finaliza activity

### Roles y restricciones

- Si usuario actual es `COORDINATOR`:
  - spinner solo permite `Empleado`.
  - spinner deshabilitado.
- Si usuario actual es `ADMIN`:
  - permite crear `Administrador`, `Coordinador` o `Empleado`.
- Validacion adicional: coordinador no puede forzar otro rol.

### Flujo tecnico de alta

- Usa `FirebaseApp` secundario (`RegisterSecondaryApp`) para crear usuario sin cerrar sesion actual del admin.
- `FirebaseAuth` secundario llama `createUserWithEmailAndPassword`.
- Si crea credenciales, persiste perfil en `Usuarios/{uid}` con:
  - `uid`, `nombre`, `apellido`, `correo`, `role`, `active=true`.
- Despues de guardar:
  - hace `signOut()` del auth secundario
  - `secondaryApp.delete()`
  - vuelve a dashboard

### UX de teclado

- Scroll inteligente para mantener campos visibles con IME abierto.
- `OnGlobalLayoutListener` para detectar teclado y ajustar scroll.

## 5) Detalle Activos y Nuevos

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/activenew/ActiveNewUsersActivity.java`

### Filtros de rango

- Selector inicio/fin por anio y mes (4 spinners).
- Si rango invalido (`start > end`), ajusta automaticamente fin = inicio y muestra `Toast`.
- Rango por defecto: ultimos 3 meses disponibles.

### KPIs calculados

Sobre `filtered` (periodos dentro del rango):

- Total de activos.
- Total de nuevos.
- Promedio mensual de activos.
- Promedio mensual de nuevos.
- Captacion = `totalNew / totalActive`.
- Variacion activos ultimo mes vs anterior.
- Variacion nuevos ultimo mes vs anterior.
- Insight cualitativo basado en:
  - direccion de activos y nuevos
  - umbral de captacion (`> 0.35`).

### Desglose y visual

- Lista mensual (`item_monthly_active_new.xml`) con:
  - periodo
  - activos/nuevos
  - barra de progreso normalizada por maximo de activos del rango

### Exportacion CSV

- Genera archivo en cache: `cache/exports/active_new_<timestamp>.csv`.
- Columnas:
  - `Year,Month,ActiveUsers,NewUsers,CaptureRatePercent`
- Usa `FileProvider` (`${applicationId}.fileprovider`) para compartir por `Intent.ACTION_SEND`.

## 6) Detalle Vistas por Mes

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/viewspermonth/ViewsPerMonthActivity.java`

### Filtros de rango

- Misma mecanica de rango que Active/New.
- Rango invalido se corrige automaticamente.

### KPIs calculados

- Total de vistas.
- Promedio mensual de vistas.
- Mes pico (max visitas).
- Mes mas bajo (min visitas).
- Variacion entre primer y ultimo mes del rango.
- Insight de comportamiento:
  - crecimiento significativo (`variation > 0.2`)
  - caida (`variation < -0.2`)
  - volatilidad (muchos meses en valle)
  - estable

### Desglose y visual

- Lista mensual (`item_views_per_month.xml`) con barra por visitas normalizada al maximo.

### Exportacion CSV

- `cache/exports/views_month_<timestamp>.csv`.
- Columnas:
  - `Year,Month,Visits,Sessions,ActiveUsers,NewUsers,EngagementRatePercent,TopSource`
- Incluye escape CSV para `TopSource` (`"..."` con dobles comillas internas escapadas).

## 7) Administracion de usuarios

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/usermanagement/UserManagementActivity.java`

### Seguridad

- Pantalla protegida con `requireAdminOnly()`.
- Si no cumple rol admin, muestra mensaje y `finish()`.

### Lectura y resumen

- Observa nodo `Usuarios` en tiempo real con `ValueEventListener`.
- Convierte cada registro a `AppUser`.
- Ordena por nombre para visualizacion.
- Muestra contadores:
  - total, activos, inactivos
  - admins, coordinadores, empleados

### Edicion de usuario

- Tap sobre item abre dialog (`dialog_user_management.xml`).
- Campos editables:
  - rol (spinner)
  - estado activo (`SwitchCompat`)
- Guardado con `updateChildren` en `Usuarios/{uid}` preservando nombre, apellido, correo y uid.

### Presentacion

- Items en `ListView` (`item_user_management.xml`) con badge visual activo/inactivo.

## 8) Perfil de usuario

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/profile/ProfileActivity.java`

### Datos editables

- Nombre
- Apellido
- Telefono
- Profesion/carrera
- Fecha de nacimiento (DatePicker)
- Instagram
- Foto de perfil

### Carga inicial

- Lee `Usuarios/{uid}`.
- Mapea datos de perfil y compatibilidad retro:
  - si `instagram` vacio, intenta leer `redesSociales`.
- Carga `profileImageBase64` si existe.

### Guardado

- Valida campos obligatorios: nombre, apellido, telefono, profesion, fecha.
- Escribe en DB:
  - `nombre`, `apellido`, `telefono`, `profesion`, `fechaNacimiento`, `instagram`, `redesSociales`, `profileImageBase64`.
- Actualiza `SessionManager` con nombre y apellido nuevos.

### Flujo de imagen

- Fuente de imagen:
  - camara (`TakePicturePreview`)
  - galeria (`GetContent`)
- Procesamiento:
  - escala max `256px`
  - compresion JPEG ajustando calidad (inicia en 70, reduce hasta 35)
  - limite aprox `70 KB`
  - almacenamiento en Base64 (`NO_WRAP`) en Realtime Database

### UX de teclado

- Auto scroll por foco e IME (similar a Register).

## 9) Informacion de app

- `AboutAppActivity` existe como activity dedicada.
- En la practica, `DashboardActivity` usa dialog inflando el mismo layout `activity_about_app.xml`.
- Contenido:
  - resumen funcional de la app
  - datos de desarrollador
  - version
  - enlaces a GitHub y YouTube

## 10) Utilidad de UI Edge-to-Edge

Archivo: `app/src/main/java/com/almaquinta/analytics/iu/common/SystemBarsEdgeToEdge.java`

- Habilita `EdgeToEdge.enable(activity)`.
- Configura status/nav bar transparentes.
- Controla contraste en Android Q+.
- Permite aplicar insets a vistas objetivo y opcionalmente incluir insets de teclado (IME).

---

## Flujo de datos o logica principal

## Flujo de autenticacion y sesion

1. App inicia en `SplashActivity`.
2. Se consulta usuario actual de FirebaseAuth.
3. Si existe, se busca perfil en `Usuarios/{uid}`.
4. Si el perfil no existe o esta inactivo (`active=false`), se cierra sesion y se redirige a `MainActivity`.
5. Si el perfil es valido y activo, se parsea `AppUser` y se persiste en `SessionManager`.
6. Se redirige a `DashboardActivity`.
7. Si no existe sesion/perfil, se va a `MainActivity`.

## Flujo de autorizacion

- Cualquier pantalla sensible valida rol con `AuthorizationService`.
- Fuente de verdad para rol en runtime: `SessionManager.currentUser`.
- Regla efectiva:
  - `isAdmin()` => ADMIN y COORDINATOR
  - `isAdminOnly()` => ADMIN

## Flujo de analitica

1. `DashboardActivity`/pantallas de detalle crean `AnalyticsRepositoryImpl`.
2. Repositorio se suscribe a `estadisticas_sesiones`.
3. Lee dos colecciones:
   - `vistas_x_mes` (visitas y sesiones)
   - `activosYNuevos` (activos, nuevos, tasa, fuente)
4. Convierte mes en texto espanol a entero (`enero..diciembre` -> `1..12`).
5. Agrega por `period = year-month`.
6. Calcula tasas ponderadas por usuarios activos.
7. Construye:
   - lista `AnalyticsSummary` ordenada desc (anio/mes)
   - `Map<period, List<SourceMetric>>`
8. La UI agrega de nuevo para cada necesidad de presentacion (dashboard o detalle por rango).

## Flujo de exportacion CSV

1. Usuario presiona boton exportar en pantalla de detalle.
2. Se valida que haya data filtrada.
3. Se crea carpeta temporal `cache/exports`.
4. Se escribe CSV UTF-8.
5. Se comparte con `Intent.ACTION_SEND` y URI de `FileProvider`.

## Flujo de perfil de imagen

1. Usuario elige camara o galeria.
2. Se obtiene `Bitmap`.
3. Se redimensiona y comprime.
4. Se codifica Base64.
5. Se actualiza DB y preview local.
6. Dashboard recarga avatar al reanudar (`onResume`).

---

## Configuracion y variables de entorno

## Archivos de configuracion

- `local.properties`:
  - contiene `sdk.dir` local (ruta SDK Android).
  - no debe versionarse en repositorios publicos.
- `app/google-services.json`:
  - datos de proyecto Firebase (`project_id`, `firebase_url`, `mobilesdk_app_id`, `api_key`, etc.).
- `app/src/main/AndroidManifest.xml`:
  - activities declaradas.
  - `FileProvider` para exportaciones.
  - backup rules (`data_extraction_rules`, `backup_rules`).
- `app/src/main/res/xml/file_paths.xml`:
  - expone solo `cache/exports/` a traves de `FileProvider`.

## Nodos de Realtime Database esperados

### `Usuarios/{uid}`

Campos utilizados por distintas pantallas:

- `uid` (String)
- `nombre` (String)
- `apellido` (String)
- `correo` (String)
- `role` (String: ADMIN/COORDINATOR/EMPLOYEE)
- `active` (Boolean/String parseable)
  - comportamiento actual de seguridad operativa: `false` bloquea ingreso tanto en `SplashActivity` como en `MainActivity`.
- `telefono` (String)
- `profesion` (String)
- `fechaNacimiento` (String `dd/MM/yyyy`)
- `instagram` (String)
- `redesSociales` (String, compatibilidad)
- `profileImageBase64` (String)

### `estadisticas_sesiones`

Subnodos usados:

- `vistas_x_mes[]`:
  - `AÑO`
  - `MES` (texto espanol)
  - `VISITAS`
  - `SESIONES DE USUARIOS`
- `activosYNuevos[]`:
  - `AÑO`
  - `MES`
  - `USUARIOS ACTIVOS`
  - `NUEVOS USUARIOS`
  - `TASA DE INTERACCIÓN` (con `%` opcional)
  - `FUENTE`

## Parametros internos y constantes

- `DashboardActivity.DASHBOARD_MONTH_LIMIT = 3`
- `RegisterActivity.KEYBOARD_THRESHOLD_DP = 120`
- `ProfileActivity.KEYBOARD_THRESHOLD_DP = 120`
- `ProfileActivity.IMAGE_MAX_BYTES = 70 * 1024`
- `ProfileActivity.IMAGE_MAX_DIMENSION = 256`

---

## Instrucciones de instalacion y ejecucion

## Prerrequisitos

- Windows, Linux o macOS con:
  - JDK 11
  - Android Studio actualizado
  - Android SDK con plataforma 36 y build-tools compatibles
- Cuenta/proyecto Firebase con Authentication (Email/Password) y Realtime Database habilitados.

## Paso a paso desde cero

1. Clonar repositorio.
2. Abrir carpeta `alma` en Android Studio.
3. Verificar `local.properties` con ruta de SDK valida (`sdk.dir=...`).
4. Colocar `app/google-services.json` correcto para tu paquete `com.almaquinta.analytics`.
5. Sincronizar Gradle (`Sync Project with Gradle Files`).
6. Confirmar que el dispositivo/emulador usa API >= 24.
7. Ejecutar configuracion `app` en modo debug.

## Comandos CLI opcionales

```powershell
cd "D:\Proyectos GitHub\alma"
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

Para instalar en dispositivo conectado:

```powershell
cd "D:\Proyectos GitHub\alma"
.\gradlew.bat installDebug
```

## Notas de ejecucion

- Se requiere conectividad a Firebase para autenticacion, carga de usuarios y metricas.
- Si no existe perfil `Usuarios/{uid}` para una cuenta autenticada, splash fuerza logout y envia a login.
- Si `Usuarios/{uid}.active=false`, la app invalida sesion y bloquea el acceso a dashboard (en splash y login).

---

## Decisiones tecnicas relevantes

## 1) Uso de Firebase Realtime Database como backend unico

Se eligio un backend realtime para simplificar sincronizacion de metricas y usuarios sin crear API intermedia.

Impacto:

- Menor complejidad de backend propio.
- Dependencia fuerte de esquema de nodos y calidad de datos en RTDB.

## 2) Repositorio de analitica unificado (`AnalyticsRepositoryImpl`)

Toda la logica de parseo/agregacion de metricas se concentra en un repositorio.

Impacto:

- Evita duplicar parseos en Activities.
- UI consume objetos de dominio (`AnalyticsSummary`, `SourceMetric`) en vez de snapshots crudos.

## 3) Control de sesion en memoria (`SessionManager`)

Se usa singleton simple para evitar lecturas repetidas de perfil entre pantallas.

Impacto:

- Navegacion rapida y chequeo de rol centralizado.
- La sesion en memoria se pierde si proceso se mata (se recompone en Splash/Login).

## 4) Registro con `FirebaseApp` secundario

Crear usuarios desde app admin con `createUserWithEmailAndPassword` sobre auth principal cambiaria la sesion activa. Se evita con app/auth secundarios temporales.

Impacto:

- Mantiene sesion del administrador intacta.
- Requiere limpieza explicita (`signOut` + `delete`) para no dejar instancias colgadas.

## 5) Edge-to-edge + manejo manual de insets/teclado

Se centraliza en helper (`SystemBarsEdgeToEdge`) y en logica de auto scroll en formularios.

Impacto:

- Mejor experiencia visual moderna.
- Mayor complejidad de UI en formularios largos.

## 6) Exportacion por cache + FileProvider

Se evita almacenamiento externo directo y permisos adicionales usando `cache/exports` y URI segura.

Impacto:

- Flujo compatible con politicas modernas de Android.
- Archivos temporales no persistentes por diseno.

---

## Limitaciones conocidas o deuda tecnica

## Seguridad

1. `app/google-services.json` contiene `api_key` y metadatos de Firebase en el repo.
   - Esto es habitual en Firebase, pero requiere reglas de seguridad estrictas en backend.
2. No se observan reglas de Firebase en repositorio.
   - Sin reglas robustas, un cliente modificado podria leer/escribir nodos sensibles.
3. Asignacion implicita de `ADMIN` cuando `role` esta vacio (`MainActivity` y `SplashActivity`).
   - Riesgo de elevacion de privilegios por datos incompletos o corruptos.
4. No hay doble validacion server-side visible de permisos para cambios de rol/estado.
   - La restriccion principal esta del lado cliente.

## Arquitectura y mantenibilidad

1. Logica de negocio repartida en Activities (sumatorias, validaciones, reglas de insights).
2. Duplicacion de logicas entre `ActiveNewUsersActivity` y `ViewsPerMonthActivity` (filtros/rango/export base).
3. `CreateUserByAdminUseCase` no esta integrado al flujo real.
4. `AuthorizationService` tiene metodos redundantes (`requireRegistrationAccess` y `requireAdmin` hacen lo mismo).
5. Algunos textos y defaults estan hardcodeados en Java en lugar de `strings.xml`.

## Calidad y testing

1. No hay pruebas unitarias funcionales de negocio (solo tests plantilla de Android Studio).
2. No hay pruebas instrumentadas de flujos criticos (login, roles, registro, exportaciones).
3. No hay pipeline de CI definido en el repositorio.

## Comportamiento funcional

1. En `DashboardActivity`, los filtros de spinners (anio/mes) no afectan realmente el subconjunto; `filterSummaries()` toma solo ultimos 3 meses.
2. Orden de anios en vistas de detalle depende de insercion del `LinkedHashSet`; no hay sort explicito descendente.
3. Exportacion CSV usa nombres de mes localizados (Espanol) en vez de numericos normalizados.
4. Formato de fecha de perfil es texto libre con DatePicker, sin validacion semantica adicional.

## UI/UX y accesibilidad

1. Varias `contentDescription` usan string genérica (`todo`) en imagenes decorativas.
2. Algunos contrastes y textos pueden requerir auditoria WCAG.
3. Layouts extensos en XML (especialmente dashboard) dificultan mantenibilidad.

## Performance

1. Realtime listeners observan nodos completos y re-agregan en memoria cada cambio.
2. Sin paginacion ni consultas parciales por rango.
3. Avatares Base64 en RTDB pueden crecer y afectar payload.

---

## Archivos clave por funcionalidad

- Autenticacion: `app/src/main/java/com/almaquinta/analytics/iu/main/MainActivity.java`
- Ruteo inicial: `app/src/main/java/com/almaquinta/analytics/iu/main/SplashActivity.java`
- Dashboard: `app/src/main/java/com/almaquinta/analytics/iu/dashboard/DashboardActivity.java`
- Repositorio analitico: `app/src/main/java/com/almaquinta/analytics/data/repository/AnalyticsRepositoryImpl.java`
- Registro de staff: `app/src/main/java/com/almaquinta/analytics/iu/register/RegisterActivity.java`
- Detalle activos/nuevos: `app/src/main/java/com/almaquinta/analytics/iu/activenew/ActiveNewUsersActivity.java`
- Detalle vistas/mes: `app/src/main/java/com/almaquinta/analytics/iu/viewspermonth/ViewsPerMonthActivity.java`
- Gestion de usuarios: `app/src/main/java/com/almaquinta/analytics/iu/usermanagement/UserManagementActivity.java`
- Perfil: `app/src/main/java/com/almaquinta/analytics/iu/profile/ProfileActivity.java`
- Seguridad/roles: `app/src/main/java/com/almaquinta/analytics/security/AuthorizationService.java`
- Sesion: `app/src/main/java/com/almaquinta/analytics/session/SessionManager.java`
- Manifest/provider: `app/src/main/AndroidManifest.xml`
- Strings globales: `app/src/main/res/values/strings.xml`
- Config Firebase: `app/google-services.json`

---

## Estado actual de pruebas

- `app/src/test/java/com/almaquinta/analytics/ExampleUnitTest.java`:
  - test basico `2 + 2 = 4`.
- `app/src/androidTest/java/com/almaquinta/analytics/ExampleInstrumentedTest.java`:
  - valida package name.

No existe cobertura de reglas de negocio ni flujos de UI.

---

## Licencia y uso

No se encontro archivo de licencia (`LICENSE`) en la raiz del repositorio. Si el proyecto se distribuira publicamente, conviene explicitar una licencia de uso.
