# Bundled fonts

Subsetted to Latin (other scripts fall back to the phone's font).

| Font | Files | Licence |
|---|---|---|
| Geist (Vercel / Basement Studio) | `res/font/geist_variable.ttf` (variable) | SIL Open Font License 1.1 |
| Geist Mono (Vercel / Basement Studio) | `res/font/geist_mono_variable.ttf` (variable) | SIL Open Font License 1.1 |
| Doto (Óliver Lalan) | `res/font/doto_variable.ttf` (variable) | SIL Open Font License 1.1 |
| Inter Display (Rasmus Andersson) | `res/font/inter_display_*.otf` | SIL Open Font License 1.1 |
| Lora (Cyreal) | `res/font/lora_*.ttf` | SIL Open Font License 1.1 |
| Poppins (Indian Type Foundry) | `res/font/poppins_*.ttf` | SIL Open Font License 1.1 |

Geist, Geist Mono and Doto are the Nothing OS-style stand-ins: Geist for UI text, Geist Mono for
monospace readouts, Doto for dot-matrix numerals. All are free to use, change and redistribute
under the SIL Open Font License 1.1 (<https://openfontlicense.org>). No proprietary Nothing
typefaces (NDot, NType, Lettera Mono) are bundled.

Doto is drawn at a capped weight (≈500) so its dots stay separate and round; heavier requests
render at 500.

## Your own fonts

Settings > Appearance > Island font > **Add font (.ttf or .otf)** copies a font file you choose into the app's private
storage (`files/fonts`). Those fonts are never part of this repository or of a build, and they are covered by their own
licences: add only fonts you are allowed to use. They are not included in a settings backup.
