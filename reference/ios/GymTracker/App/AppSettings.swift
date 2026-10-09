import Foundation
import SwiftUI

/// Weight units used across the app. Stored values are always pounds;
/// the setting only changes how values are entered and displayed.
enum WeightUnit: String, CaseIterable, Identifiable {
    case lb, kg

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .lb: return "Pounds (lb)"
        case .kg: return "Kilograms (kg)"
        }
    }

    var abbreviation: String {
        switch self {
        case .lb: return "lb"
        case .kg: return "kg"
        }
    }

    /// Pounds per one of this unit.
    var poundsPerUnit: Double {
        switch self {
        case .lb: return 1
        case .kg: return 2.20462
        }
    }

    /// Converts a stored pound value into this unit for display/entry.
    func fromPounds(_ pounds: Double) -> Double {
        pounds / poundsPerUnit
    }

    /// Converts an entered value in this unit back to stored pounds.
    func toPounds(_ value: Double) -> Double {
        value * poundsPerUnit
    }
}

/// Rest timer options for the setting (actual timer UI comes later;
/// the stored default is ready for it).
enum RestTimerOption: Int, CaseIterable, Identifiable {
    case off = 0
    case seconds60 = 60
    case seconds90 = 90
    case seconds120 = 120
    case seconds180 = 180

    var id: Int { rawValue }

    var displayName: String {
        switch self {
        case .off: return "Off"
        default: return "\(rawValue)s"
        }
    }
}

/// App-wide user preferences, persisted with AppStorage. No model
/// migration needed — everything lives in UserDefaults.
struct AppSettings {

    // MARK: - Keys

    private enum Keys {
        static let weightUnit = "settings.weightUnit"
        static let appearance = "settings.appearance"
        static let restTimer = "settings.restSeconds"
        static let keepAwake = "settings.keepAwakeDuringWorkout"
        static let confirmDiscard = "settings.confirmBeforeDiscarding"
    }

    // MARK: - Backing storage
    // NOTE: @AppStorage can't be static, so these read/write UserDefaults
    // directly. Views observe the same keys with @AppStorage (non-static).

    private static var defaults: UserDefaults { .standard }

    static var weightUnitRaw: String {
        get { defaults.string(forKey: Keys.weightUnit) ?? WeightUnit.lb.rawValue }
        set { defaults.set(newValue, forKey: Keys.weightUnit) }
    }
    static var appearanceRaw: String {
        get { defaults.string(forKey: Keys.appearance) ?? AppearanceSetting.system.rawValue }
        set { defaults.set(newValue, forKey: Keys.appearance) }
    }
    static var restSeconds: Int {
        get {
            // integer(forKey:) returns 0 when unset — 0 == .off, the default.
            defaults.integer(forKey: Keys.restTimer)
        }
        set { defaults.set(newValue, forKey: Keys.restTimer) }
    }
    static var keepAwakeDuringWorkout: Bool {
        get { defaults.bool(forKey: Keys.keepAwake) }
        set { defaults.set(newValue, forKey: Keys.keepAwake) }
    }
    static var confirmBeforeDiscarding: Bool {
        get {
            // bool(forKey:) returns false when unset — default is true, so
            // fall back to true unless the key exists.
            defaults.object(forKey: Keys.confirmDiscard) as? Bool ?? true
        }
        set { defaults.set(newValue, forKey: Keys.confirmDiscard) }
    }

    // MARK: - Typed accessors

    static var weightUnit: WeightUnit {
        get { WeightUnit(rawValue: weightUnitRaw) ?? .lb }
        set { weightUnitRaw = newValue.rawValue }
    }

    static var appearance: AppearanceSetting {
        get { AppearanceSetting(rawValue: appearanceRaw) ?? .system }
        set { appearanceRaw = newValue.rawValue }
    }

    static var restTimer: RestTimerOption {
        get { RestTimerOption(rawValue: restSeconds) ?? .off }
        set { restSeconds = newValue.rawValue }
    }

    /// Resets every setting back to its default.
    static func resetAll() {
        weightUnit = .lb
        appearance = .system
        restTimer = .off
        keepAwakeDuringWorkout = false
        confirmBeforeDiscarding = true
    }
}

/// Light/dark mode override.
enum AppearanceSetting: String, CaseIterable, Identifiable {
    case system, light, dark

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }

    /// nil = follow the system.
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}
