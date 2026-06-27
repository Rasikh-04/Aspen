# iOS CI runner

**Nothing to set up.** The `ios` job in `.github/workflows/ci.yml` runs on a
**GitHub-hosted macOS runner** (`runs-on: macos-14`). GitHub provisions the Mac,
Xcode, and the Apple toolchain automatically on every push/PR — no registration,
no machine to manage.

## Why not self-hosted / Linux

Kotlin/Native's iOS targets (`iosSimulatorArm64`, `iosArm64`, `iosX64`) require
Xcode's Apple toolchain, which only exists on macOS. There is **no Linux
cross-compile path**, so the iOS framework link cannot run on the Android/Linux
job.

> If you tried to register a self-hosted runner and hit
> `cannot execute binary file: Exec format error` / missing `libcoreclr.so`,
> that's because the **macOS** runner binary was run on a **Linux** host. A
> self-hosted macOS runner must run on an actual Mac. We use a GitHub-hosted Mac
> instead, so this is moot.

## Cost note

macOS minutes bill at ~10× Linux on **private** repos (free on public repos).
The `ios` job only compiles + links the shared framework, so it's short. If
private-repo minutes become a concern, options are: make the `ios` job run only
on `main` / on a label, or move it to a self-hosted Mac later (revert `runs-on`
to `[self-hosted, macos]` and register a runner **on a Mac**).
