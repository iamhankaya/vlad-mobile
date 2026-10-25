# AGENTS.md — vlad-mobile

VLAD Android uygulaması (Kotlin, Compose). Modüller:
- `:core`: saf JVM. Kanonik imza baytları, katı DER ve low-S imza, API modelleri, reducer, SSE ayrıştırıcı.
- `:app`: Android uygulaması.
- `contract/`: vlad reposundan kopyalanır, elle düzenlenmez.

**Repo PUBLIC.** Şunlar repoya asla girmez: `google-services.json`, keystore (debug dahil), gerçek sunucu adresi ya da IP, anahtar, token.

Kontroller:
```
./gradlew -PskipAndroid=true :core:test
./gradlew lintDebug testDebugUnitTest assembleDebug
./gradlew :core:ktlintCheck :core:detekt :app:ktlintCheck
```

## Review guidelines

PR incelemesinde aşağıdaki skill dosyalarını oku ve ölçüt olarak uygula:

1. **Genel kod incelemesi:** `.claude/skills/requesting-code-review/code-reviewer.md`. Plan ve gereksinim uyumunu, kod kalitesini, testleri ve önem derecelendirmesini buna göre değerlendir.
2. **Güvenlik:** `.claude/skills/security-audit/SKILL.md` ile birlikte `DESKTOP-MOBILE-AND-LOCAL-IPC.md`, `WEB-PROTOCOL-AND-AUTH.md` ve `CLIENT-SIDE.md`. Yalnız PR diff'inin dokunduğu yollara odaklan; tam denetim yapma.
3. **Test disiplini:** `.claude/skills/test-driven-development/SKILL.md` ve `.claude/skills/verification-before-completion/SKILL.md`. Davranış değiştiren her değişikliğin bir testi olmalı. Testsiz "düzeltildi" iddiasını bulgu say.
4. **UI değişikliği (Compose ekranları):** `.claude/skills/impeccable/SKILL.md` ve `.claude/skills/ui-ux-pro-max/SKILL.md`. Erişilebilirliğe, dokunma hedeflerine ve okunurluğa bak. Varsa `docs/design-audit/` bulgularıyla tutarlılığı kontrol et.

P0/P1 sayılacaklar (bu repoya özgü):
- Repoya sır, sunucu adresi, `google-services.json`, keystore ya da imza anahtarı girmesi.
- İmza yolunun bozulması:
  - Kanonik bayt üretimi, low-S ve katı DER kuralları `contract/signing-vectors.json` ile çelişiyor.
  - Onay anahtarı biyometri veya cihaz kimlik doğrulaması olmadan kullanılabiliyor. Karar: biyometri veya PIN/desen ile `setUnlockedDeviceRequired`.
  - Onay ekranında gösterilen alanlar imzalanan challenge'dan gelmiyor.
- Hassas ekranlarda `FLAG_SECURE` eksik. Hassas ekranlar: onay, eşleştirme, sohbet.
- Hassas verinin loglanması: token, challenge, imza, sunucu adresi.
- Exported component, deep link ya da intent üzerinden doğrulamasız eylem.
- Ağ güvenliği gevşetilmesi: cleartext, kullanıcı CA'sı, hostname doğrulamasının kapatılması.
- `contract/` dosyalarının elle değiştirilmesi.
- Testin atlanması, devre dışı bırakılması ya da zayıflatılması.

Stil (ktlint/detekt'in zaten yakaladıkları) ve isimlendirme P2 ya da altıdır; bunları kısa tut. Yorumları Türkçe yaz.
