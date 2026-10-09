import SwiftUI

/// Central design system for the app.
///
/// Colors, spacing and corner radii live here so the whole app can be
/// re-themed from a single place. System colors adapt to light/dark mode
/// automatically; custom tints are chosen to read well in both.
enum Theme {

    // MARK: - Colors

    /// Main accent color used for primary actions and highlights.
    static let primary = Color(red: 0.22, green: 0.78, blue: 0.47)

    static let success = Color.green
    static let warning = Color.orange
    static let destructive = Color.red

    /// Secondary text (captions, hint labels).
    static let secondaryText = Color.secondary

    /// Screen background. Adapts automatically to light and dark mode.
    static let background = Color(.systemGroupedBackground)

    /// Surface used for cards.
    static let cardBackground = Color(.secondarySystemBackground)

    /// Fill used behind text fields and small controls.
    static let fieldBackground = Color(.tertiarySystemFill)

    // MARK: - Metrics

    static let padding: CGFloat = 16
    static let spacing: CGFloat = 12
    static let cornerRadius: CGFloat = 14
    static let cardCornerRadius: CGFloat = 18
}

/// Applies the standard rounded card look used throughout the app.
struct CardStyle: ViewModifier {
    var cornerRadius: CGFloat = Theme.cardCornerRadius

    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(Theme.cardBackground)
            )
    }
}

extension View {
    /// Gives a view the standard card styling used across the app.
    func cardStyle(cornerRadius: CGFloat = Theme.cardCornerRadius) -> some View {
        modifier(CardStyle(cornerRadius: cornerRadius))
    }
}

extension Text {
    /// Small, uppercase label used for section headers.
    func sectionHeaderStyle() -> some View {
        self
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.secondaryText)
            .textCase(.uppercase)
    }
}