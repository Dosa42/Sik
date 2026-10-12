# User APK (sadece client) — bagimsiz tam build

Tek ekran, tek girdi: NGROK URL. Girip klavyede "Git"e basinca `libuser_core.so` icindeki
`ClientSession` o adrese WebSocket ile baglanir. Durum/mesajlar sadece logcat'e yazilir (etiket: `user_core`).

Bu zip **kendi basina** bir Gradle projesidir: baska hicbir projenin dosyasina, version catalog'una
veya ayarina bagli degildir.

## Icerik
| Yol | Ne |
|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` | Kok proje |
| `gradle/libs.versions.toml` | Kendi version catalog'u (AGP surumu burada) |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/` | Gradle wrapper (9.6.0) |
| `app_user/` | Uygulama modulu (Kotlin kabuk + `src/main/cpp` JNI) |
| `native/` | `client_lib` + `third_party` (IXWebSocket, mbedTLS v3.6.7) |
| `.github/workflows/build-apk.yml` | GitHub Actions: APK build + ayrim kontrolu + artifact |

## Sabit surumler (birbirine bagli, birlikte degistir)
| Arac | Surum |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Gradle (wrapper) | 9.6.0 (AGP 9.4 icin minimum) |
| JDK | 17 |
| SDK Build Tools | 36.0.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| NDK | 28.2.13676358 |
| CMake | 3.22.1 |

## GitHub'da build (onerilen)
1. Bu klasorun **icindekileri** yeni bir repo'nun kokune koy (`.github/` repo kokunde olmali) ve push et.
2. Actions sekmesi -> **Build User APK** otomatik calisir (veya "Run workflow").
3. Bitince run sayfasinin altindaki **Artifacts -> user-apk** icinden debug ve release APK'yi indir.
4. `v1.0` gibi bir etiket (tag) atarsan APK'lar GitHub Release olarak da yayinlanir.

Release APK varsayilan olarak debug anahtariyla imzalanir (telefona kurulur, Play Store'a yuklenmez).
Kendi anahtarinla imzalamak icin repo Secrets: `USER_APK_KEYSTORE_BASE64` (keystore'un base64'u),
`USER_APK_KEYSTORE_PASSWORD`, `USER_APK_KEY_ALIAS`, `USER_APK_KEY_PASSWORD`.

## Yerelde build
JDK 17 + Android SDK (Platform 36, Build-Tools 36.0.0, NDK 28.2.13676358, CMake 3.22.1) kuruluyken:

    echo "sdk.dir=/yol/Android/Sdk" > local.properties
    ./gradlew :app_user:assembleDebug
    # APK: app_user/build/outputs/apk/debug/app_user-debug.apk

Android Studio ile: klasoru "Open" et, Gradle senkronu bitince Run.

## Adres girdisi
`xxx.ngrok-free.app` | `https://...` | `wss://...` (TLS)  ve  `http://IP:8085` | `ws://IP:8085` (LAN)

## Ayrim kontrolu (release APK'da bos cikmali — CI bunu otomatik yapar)
    unzip -o app_user-release.apk 'lib/*' -d /tmp/u
    nm -D /tmp/u/lib/arm64-v8a/libuser_core.so | grep -iE "admin|permission|setSetting|createUser"
    unzip -l app_user-release.apk | grep -i ngrok

## Admin projesine eklemek istersen
Bu zip bagimsiz oldugu icin eklemek zorunda degilsin. Yine de ayni projeye almak istersen:
`app_user/` ve `native/` klasorlerini kopyala, `settings.gradle.kts`'e `include(":app_user")` ekle,
`app_user/build.gradle.kts` icindeki `alias(libs.plugins.android.application)` satirinin senin
catalog'unda karsiligi olmali.
