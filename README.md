# Polishändelser (Android)

Enkel Android-app (Kotlin + Jetpack Compose) som visar aktuella händelser från Polisens öppna API:
`GET https://polisen.se/api/events`

## Funktioner (v0.2)
- Startskärm med inmatningsfält för **ort** (kommun eller län) + förslag ur aktuellt flöde
- Lista med händelser för vald ort, **nyast klockslag först** (händelsetid ur rubriken, inte publiceringstid)
- Pull-to-refresh
- Detaljvy med länk till hela texten på polisen.se
- Felhantering: helskärmsfel med "Försök igen" vid tom lista, snackbar om listan redan finns
- Ljust/mörkt tema, dynamiska färger (Android 12+)

## Teknik
| Del | Val |
|---|---|
| Språk | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 (BOM 2024.12.01) |
| Nätverk | Retrofit 2.11 + Gson |
| Navigation | navigation-compose 2.8.5 |
| Bygg | AGP 8.7.3, Gradle 8.10.2 |
| SDK | minSdk 26, target/compileSdk 35 |

## Struktur
```
app/src/main/java/se/polishandelser/app/
├── MainActivity.kt            Navigation + tema
├── data/
│   ├── PoliceApi.kt           Retrofit-interface + ApiModule (OkHttp, User-Agent)
│   ├── PoliceRepository.kt
│   └── model/PoliceEvent.kt   Datamodell + datum-/GPS-tolkning
└── ui/
    ├── EventsViewModel.kt     UI-state (loading/refreshing/error)
    ├── OrtScreen.kt           Startskärm: ortfält + förslag
    ├── EventsScreen.kt        Lista för ort + pull-to-refresh
    └── EventDetailScreen.kt   Detaljvy
```

## Bygga
**Android Studio:** Öppna mappen → vänta på Gradle sync → Run.

**Kommandorad:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

**GitHub:** Varje push till `main` bygger en debug-APK och publicerar den som en **Release** (Releases till höger på repots startsida → ladda ner `.apk`).

## Steg 1 – lägga upp på GitHub
```bash
cd PolisHandelser
git init
git add .
git commit -m "Första version: lista, pull-to-refresh, detaljvy"
git branch -M main
git remote add origin https://github.com/<ditt-konto>/PolisHandelser.git
git push -u origin main
```

## Steg 2 – förberedelser för Google Play (att göra)
1. **applicationId** – byt `se.polishandelser.app` till ett eget (t.ex. `se.<dittföretag>.polishandelser`) *innan* första uppladdning. Går inte att ändra efteråt.
2. **Ikon och namn** – ersätt platshållarikonen; Play kräver även en 512×512-ikon och en 1024×500 feature graphic.
3. **Signering** – skapa en uppladdningsnyckel (`keytool`), lagra den utanför git, aktivera *Play App Signing*.
4. **Release-bygge** – `./gradlew bundleRelease` (AAB, inte APK). Slå på `isMinifyEnabled = true` och testa release-bygget på riktig enhet (Gson + R8 är vanlig felkälla – regler finns i `proguard-rules.pro`).
5. **Integritetspolicy** – krävs av Play även om appen inte samlar in data. Behöver en publik URL.
6. **Data safety-formulär** – appen samlar inget, men kommunicerar med polisen.se.
7. **Nytt privat utvecklarkonto** – Google kräver *closed testing* med minst 12 testare i 14 dagar innan produktion kan sökas. (Gäller ej organisationskonton med D-U-N-S.)
8. **Avsändarneutralitet** – appen får inte se ut som en officiell Polisen-app (Play-policy om imitation). Använd inte Polisens logotyp; ange i beskrivningen att data kommer från Polisens öppna API och att appen inte är officiell.
9. **targetSdk** – Play höjer kravet årligen; kontrollera aktuellt krav före uppladdning.

## Senare funktioner
- Karta (GPS finns redan i `Location.latLng`) – osmdroid eller Google Maps
- Filtrering på händelsetyp och län/kommun
- Favoritkommuner
- Notiser (WorkManager som pollar, max var 15:e min) för valda typer
- Hemskärmswidget (Glance)

## Om datan
- Ortfiltreringen görs i appen. API:ts `?locationname=` gav i test samma svar oavsett ort, så det används inte.
- Orten tas från sista ledet i rubriken ("…, Trafikolycka, Skövde"). Flödet innehåller bara de ~500 senaste händelserna i hela landet (några dygn), så små orter kan sakna träffar.
- API:t ger bara en kort sammanfattning; hela texten finns på händelsesidan.
- Platsen är oftast **län** med länets mittpunkt-koordinat, inte exakt plats – viktigt att tänka på för en karta.
- Polisen kan strypa klienter som anropar för ofta. Appen anropar bara vid start och vid pull-to-refresh.
