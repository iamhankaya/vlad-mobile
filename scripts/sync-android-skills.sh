#!/usr/bin/env bash
# Google'ın resmi android/skills reposundan seçili skill'leri .claude/skills/ altına eşitler.
# Kullanım: scripts/sync-android-skills.sh [ref]
#   ref verilmezse .claude/skills/ANDROID_SKILLS.md içinde kayıtlı commit kullanılır.
# Ortam: ANDROID_SKILLS_REPO (kaynak repo), SKILLS_DIR (hedef dizin) — testler için.
set -euo pipefail

repo_url="${ANDROID_SKILLS_REPO:-https://github.com/android/skills.git}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
skills_dir="${SKILLS_DIR:-$root/.claude/skills}"
registry="$skills_dir/ANDROID_SKILLS.md"
license_name="LICENSE-ANDROID-SKILLS.txt"

# Kaynak yol; hedef ad yolun son bileşenidir.
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

ref="${1:-}"
if [[ -z "$ref" ]]; then
  ref="$(sed -n 's/^Commit: `\([0-9a-f]\{7,40\}\)`.*/\1/p' "$registry" | head -n1)"
  [[ -n "$ref" ]] || { echo "ref verilmedi ve $registry içinde kayıtlı commit yok" >&2; exit 2; }
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# Tam klon: checkout yok (kaynakta Windows'ta çok uzun yollar var), yalnız nesneler gerekir.
git clone -q --no-checkout "$repo_url" "$work/src"
commit="$(git -C "$work/src" rev-parse --verify "$ref^{commit}")"

# Önce her şeyi hazırlık dizinine çıkar; bir yol eksikse hedefe dokunmadan dur.
mkdir -p "$work/stage"
for p in "${paths[@]}"; do
  name="$(basename "$p")"
  mkdir -p "$work/stage/$name"
  git -C "$work/src" archive "$commit" -- "$p" | tar -x -C "$work/stage/$name" --strip-components="$(awk -F/ '{print NF}' <<<"$p")"
  [[ -f "$work/stage/$name/SKILL.md" ]] || { echo "$p: SKILL.md yok" >&2; exit 1; }
done
git -C "$work/src" show "$commit:LICENSE.txt" > "$work/stage/$license_name"

for p in "${paths[@]}"; do
  name="$(basename "$p")"
  rm -rf "${skills_dir:?}/$name"
  mv "$work/stage/$name" "$skills_dir/$name"
done
mv "$work/stage/$license_name" "$skills_dir/$license_name"

sed -i "s/^Commit: \`[0-9a-f]\{7,40\}\`/Commit: \`$commit\`/" "$registry"
echo "android/skills @ $commit eşitlendi (${#paths[@]} skill)"
