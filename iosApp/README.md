# iosApp (SwiftUI)

A minimal SwiftUI client that links the `Shared` Kotlin Multiplatform framework.

Build on macOS with Xcode 15+:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # or embedAndSignAppleFrameworkForXcode from an Xcode build phase
```

Create an Xcode iOS App target, add the files in `PosApp/`, and link
`shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`. This client is not built in CI (that needs a macOS runner).
