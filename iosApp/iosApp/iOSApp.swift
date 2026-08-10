import SwiftUI
import Shared

@main
struct iOSApp: App {

    private let appleIntelligence = AppleIntelligenceService()

    // You found the iOS handshake. Connect Swift's AI service before the game starts.
    /* init() {
        AppleIntelligenceBridgeKt.registerAppleIntelligenceBridge(bridge: appleIntelligence)
    } */

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
