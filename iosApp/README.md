# iosApp — thin SwiftUI host

The cross-platform app lives in `:shared` (Kotlin + Compose Multiplatform). This target is a thin
SwiftUI host that embeds the shared Compose UI via `MainViewController()` (see `ContentView.swift`).

## CI gate (active from day one)
CI compiles **and links** the Kotlin/Compose iOS framework on every change:

```
./gradlew :shared:ui:linkDebugFrameworkIosSimulatorArm64
```

That is the "iOS target compiles from day one" gate (CLAUDE.md / docs/04).

## Generating the Xcode project (on a Mac, port-and-polish)
The Xcode project is generated from `project.yml` instead of a hand-maintained `.pbxproj`:

```
brew install xcodegen
cd iosApp && xcodegen generate
open iosApp.xcodeproj
```

Then add `Shared.framework` (output of the Gradle link task) to *Frameworks, Libraries, and
Embedded Content* and build. This wiring needs a Mac and is intentionally deferred to Phase 1 polish.
