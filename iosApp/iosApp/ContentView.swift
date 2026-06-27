import SwiftUI
import Shared

/// Bridges the shared Compose UI (Kotlin `MainViewController()`) into SwiftUI.
/// Per CLAUDE.md/docs/04 the iOS app is a thin host: ~90% lives in :shared.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
