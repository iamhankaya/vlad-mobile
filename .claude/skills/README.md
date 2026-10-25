# Proje skill'leri

Bulut çalışan session'ları eklenti (plugin) kuramaz. Bu yüzden rollerin kullandığı skill'ler buraya kopyalandı. Claude Code bu dizindeki skill'leri her session'da otomatik yükler. Kopyalar `iamhankaya/vlad` reposundaki `.claude/skills/` dizininden alındı ve içerikleri değiştirilmedi. VLAD'ın kendi skill'leri (`scan`, `delegate`, `status`, `digest`) yalnız vlad reposunda kullanıldığı için buraya alınmadı.

| Skill | Kaynak | Commit | Lisans |
|---|---|---|---|
| `test-driven-development`, `systematic-debugging`, `verification-before-completion`, `requesting-code-review`, `brainstorming`, `writing-plans`, `subagent-driven-development`, `executing-plans` | github.com/obra/superpowers (`skills/`) | bb92a77 | MIT |
| `ui-ux-pro-max` | github.com/nextlevelbuilder/ui-ux-pro-max-skill | 56ea9cf | MIT |
| `impeccable` | github.com/pbakaus/impeccable (`plugin/skills/impeccable`) | dad8dbb | Apache-2.0 (NOTICE.md dahil) |
| `security-audit` | github.com/cloudflare/security-audit-skill (`skills/security-audit`) | c1c8a8c | MIT |
| `design-taste-frontend` | github.com/Leonxlnx/taste-skill (`skills/taste-skill`) | 6010f91 | MIT |

Kod incelemesini Codex yapar. Codex bu skill'lerden hangilerini nasıl kullanacağını kök dizindeki `AGENTS.md` dosyasında bulur.
