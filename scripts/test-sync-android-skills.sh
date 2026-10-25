#!/usr/bin/env bash
# sync-android-skills.sh için sahte kaynak repoyla birim testi (ağ gerekmez).
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

paths=(
  jetpack-compose/jetpack-compose-performance
  jetpack-compose/adaptive
  jetpack-compose/theming/styles
  system/edge-to-edge
  navigation/navigation-3
  testing/testing-setup
  performance/r8-analyzer
  security/android-intent-security
  security/android-permissions-security
)

fail() { echo "BAŞARISIZ: $*" >&2; exit 1; }

# Sahte kaynak repo: iki commit, ikincisinde bir yan dosya değişir.
src="$tmp/src"
git init -q "$src"; git -C "$src" config core.autocrlf false
git -C "$src" config user.email t@example.invalid
git -C "$src" config user.name test
echo "Apache lisans metni" > "$src/LICENSE.txt"
for p in "${paths[@]}"; do
  mkdir -p "$src/$p/references"
  printf -- "---\nname: %s\n---\nv1\n" "$(basename "$p")" > "$src/$p/SKILL.md"
  echo "ref v1" > "$src/$p/references/a.md"
done
git -C "$src" add -A && git -C "$src" commit -q -m c1
c1="$(git -C "$src" rev-parse HEAD)"
echo "ref v2" > "$src/security/android-intent-security/references/a.md"
git -C "$src" commit -q -am c2
c2="$(git -C "$src" rev-parse HEAD)"

# Hedef: mevcut bir skill ve kayıt dosyası şablonu.
skills="$tmp/skills"
mkdir -p "$skills/brainstorming"
echo "dokunma" > "$skills/brainstorming/SKILL.md"
printf 'Commit: `0000000`\n' > "$skills/ANDROID_SKILLS.md"

run() { ANDROID_SKILLS_REPO="$src" SKILLS_DIR="$skills" bash "$here/sync-android-skills.sh" "$@" >/dev/null; }

# 1) c1 ile çek: 9 skill hedef adıyla gelir, mevcut skill'e dokunulmaz.
run "$c1"
for p in "${paths[@]}"; do
  [[ -f "$skills/$(basename "$p")/SKILL.md" ]] || fail "$p kopyalanmadı"
done
[[ "$(cat "$skills/brainstorming/SKILL.md")" == "dokunma" ]] || fail "mevcut skill değişti"
grep -q "$c1" "$skills/ANDROID_SKILLS.md" || fail "commit kaydı güncellenmedi (c1)"
[[ -f "$skills/LICENSE-ANDROID-SKILLS.txt" ]] || fail "lisans kopyalanmadı"

# 2) Aynı ref ile ikinci çalıştırma fark üretmez.
snap1="$(cd "$skills" && find . -type f -exec sha256sum {} + | sort)"
run "$c1"
snap2="$(cd "$skills" && find . -type f -exec sha256sum {} + | sort)"
[[ "$snap1" == "$snap2" ]] || fail "ikinci çalıştırma fark üretti"

# 3) c2 ile çek: içerik ve kayıtlı commit güncellenir.
run "$c2"
grep -q "ref v2" "$skills/android-intent-security/references/a.md" || fail "c2 içeriği gelmedi"
grep -q "$c2" "$skills/ANDROID_SKILLS.md" || fail "commit kaydı güncellenmedi (c2)"
grep -q "$c1" "$skills/ANDROID_SKILLS.md" && fail "eski commit kaydı kaldı"

# 4) Kaynakta olmayan yol hata verir ve hedefi bozmaz.
git -C "$src" rm -rq testing/testing-setup && git -C "$src" commit -q -m c3
c3="$(git -C "$src" rev-parse HEAD)"
if run "$c3" 2>/dev/null; then fail "eksik kaynak yolu hata vermedi"; fi
[[ -f "$skills/testing-setup/SKILL.md" ]] || fail "hata sonrası hedef bozuldu"
grep -q "$c2" "$skills/ANDROID_SKILLS.md" || fail "hata sonrası kayıt değişti"

echo "TAMAM"
