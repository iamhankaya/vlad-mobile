# vlad-mobile

VLAD Android uygulaması. **Repo PUBLIC**: `google-services.json`, keystore, gerçek sunucu adresi, anahtar ya da token buraya girmez.

- `:core` — saf JVM Kotlin (`dev.vlad.core`): kanonik imza baytları (`crypto/`), katı DER + low-S, API modelleri (`api/`), `reduce/HomeReducer`, `sse/SseParser`.
- `:app` — Android (Compose). S7a'da yalnız boş iskelet.
- `contract/` — vlad reposundan kopya (imza vektörleri, API fixture'ları); kaynak commit `contract/SOURCE.md`'de. Elle düzenlenmez.

```
./gradlew -PskipAndroid=true :core:test        # Android eklentisi/SDK'sı gerekmez
./gradlew lintDebug testDebugUnitTest assembleDebug   # ANDROID_HOME gerekir
./gradlew :core:ktlintCheck :core:detekt :app:ktlintCheck
```

CI: `.github/workflows/mobile.yml`. Debug imzası ve `google-services.json` yer tutucusu CI'da geçici üretilir.
