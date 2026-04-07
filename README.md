# Alma Quinta Growth Hub

Aplicación Android nativa (Java) para el equipo interno de Growth de Alma Quinta. Centraliza autenticación, control de acceso por roles y visualización de métricas operativas conectadas a Firebase.

## Funcionalidades principales

**Acceso y sesión:** Login con Firebase Authentication, bloqueo automático de cuentas inactivas, y ruteo inteligente desde splash según estado de sesión.

**Dashboard ejecutivo:** KPIs agregados de tráfico (visitas, sesiones, usuarios activos/nuevos, tasa de interacción, top fuente) con desglose por fuente y tendencia mensual de los últimos 3 meses.

**Análisis por rango temporal:** Vistas detalladas de activos/nuevos y vistas por mes, con filtros de rango, insights automáticos y exportación CSV compartible.

**Gestión de usuarios** *(solo administradores)*: Listado en tiempo real, edición de rol y activación/desactivación de cuentas.

**Perfil de usuario:** Edición de datos personales y foto de perfil (capturada desde cámara o galería, almacenada en Base64).

## Stack técnico

- **Lenguaje:** Java 11 — **SDK:** Android API 24–36
- **Backend:** Firebase Authentication + Realtime Database
- **UI:** Material Design, ConstraintLayout, Lottie
- **Build:** Gradle 9.4.1 con version catalog (`libs.versions.toml`)

## Roles de acceso

| Rol | Permisos |
|---|---|
| `ADMIN` | Acceso completo, incluyendo gestión de usuarios y creación de cualquier rol |
| `COORDINATOR` | Puede registrar empleados, accede a analítica |
| `EMPLOYEE` | Acceso a dashboard y perfil propio |

## Estructura del proyecto

```
app/src/main/java/com/almaquinta/analytics/
├── data/         # Modelos y repositorio de analítica (Firebase RTDB)
├── domain/       # Casos de uso
├── security/     # Guardas de autorización por rol
├── session/      # SessionManager singleton
└── iu/           # Activities por módulo (main, dashboard, profile, etc.)
```

## Configuración

1. Clonar el repositorio y abrir la carpeta `alma` en Android Studio.
2. Colocar el archivo `app/google-services.json` de tu proyecto Firebase.
3. Verificar `local.properties` con la ruta del SDK (`sdk.dir=...`).
4. Sincronizar Gradle y ejecutar en dispositivo/emulador con API ≥ 24.

```powershell
.\gradlew.bat assembleDebug   # compilar
.\gradlew.bat installDebug    # instalar en dispositivo conectado
```

## Requisitos Firebase

El proyecto requiere **Authentication** (Email/Password) y **Realtime Database** habilitados, con los nodos `Usuarios/{uid}` y `estadisticas_sesiones` poblados según el esquema esperado por la app.

---

Es conciso pero cubre todo lo esencial para que alguien externo entienda qué hace la app, cómo montarla y cómo está organizada. Si querés también puedo agregar badges (build status, API level, etc.) o una sección de capturas de pantalla.
