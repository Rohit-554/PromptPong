import SwiftUI
import Shared

@main
struct iOSApp: App {

    private let appleIntelligence = AppleIntelligenceService()

    init() {
        AppleIntelligenceBridgeKt.registerAppleIntelligenceBridge(bridge: appleIntelligence)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
