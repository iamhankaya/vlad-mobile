# contract/ kaynağı

Bu dizin `iamhankaya/vlad` reposundan **kopyalanmıştır**; burada elle düzenlenmez.

| | |
|---|---|
| Repo | `https://github.com/iamhankaya/vlad` |
| Branch | `vlad/T20261024-94` (entegrasyon branch'i, `master` değil) |
| Commit | `0d54c32` |
| Kopyalananlar | `contract/signing-vectors.json` (`vectors_version: 2`), `contract/fixtures/api/*` |
| Kopyalanmayanlar | `openapi.yaml`, `README.md` (sözleşmenin okunur kaynağı vlad reposunda), `fixtures/external/` |

Güncelleme: vlad reposunda yeni commit'ten aynı iki yolu yeniden kopyala ve yukarıdaki commit satırını değiştir.
`fixtures/api/*` `synthetic`tir (başlıkta `_fixture_header.kind`); gerçek sunucu çıktısı değildir.
`:core` testleri bu dosyaları tüketir (`-Dvlad.contract.dir`, bkz. `core/build.gradle.kts`).
