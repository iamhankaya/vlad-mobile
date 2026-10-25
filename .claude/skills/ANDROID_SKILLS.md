# Android resmi skill'leri

Google'ın resmi `android/skills` reposundan alınan skill'ler. Klasör içerikleri kaynakla birebirdir, elle düzenlenmez. Güncellemek için `scripts/sync-android-skills.sh [ref]` çalıştır; betik skill'leri yeniden çeker ve aşağıdaki commit kaydını günceller.

- Kaynak: https://github.com/android/skills
- Lisans: Apache-2.0 (metin: `LICENSE-ANDROID-SKILLS.txt`)
Commit: `4ebabf2b0fd6c04b7150023c8b612f80592a6226`

| Kaynak yol | Hedef ad |
|---|---|
| `jetpack-compose/jetpack-compose-performance` | `jetpack-compose-performance` |
| `jetpack-compose/adaptive` | `adaptive` |
| `jetpack-compose/theming/styles` | `styles` |
| `system/edge-to-edge` | `edge-to-edge` |
| `navigation/navigation-3` | `navigation-3` |
| `testing/testing-setup` | `testing-setup` |
| `performance/r8-analyzer` | `r8-analyzer` |
| `security/android-intent-security` | `android-intent-security` |
| `security/android-permissions-security` | `android-permissions-security` |

Kapsam dışı (kullanıcı kararı, 2026-10-25): xml→compose, agp-9-upgrade, android-cli ve Play skill'leri. Play yayını gündeme gelince eklenir.
