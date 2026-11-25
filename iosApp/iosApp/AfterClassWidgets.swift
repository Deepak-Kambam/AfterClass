import WidgetKit
import SwiftUI

// MARK: - Widget Timeline Entry
struct AfterClassWidgetEntry: TimelineEntry {
    let date: Date
    let quote: String
    let onlineCount: Int
    let mostActiveRoom: String
    let anonName: String
    let publicId: String
    let avatarSymbol: String
    let showOnlineCount: BooleanLiteralType
    let showIdentity: BooleanLiteralType
    let showQuickMatch: BooleanLiteralType
    let activityBars: [CGFloat]
    let streakDays: Int
}

// MARK: - Curated Nocturnal Quotes
let widgetVibeQuotes = [
    "Campus never sleeps.",
    "After hours, belong somewhere.",
    "Quiet hours. Real thoughts.",
    "Someone on campus is awake.",
    "Late night study breaks & whispers.",
    "Someone out there relates.",
    "Find your people in the quiet hours.",
    "Anonymous thoughts under the stars."
]

// MARK: - Helper Functions
func cleanPersona(_ rawName: String, _ rawTag: String) -> (name: String, tag: String) {
    let cleanName = rawName.components(separatedBy: "#").first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    let name = cleanName.isEmpty ? "Moon Fox" : cleanName
    let tagCandidate = rawName.contains("#") ? (rawName.components(separatedBy: "#").dropFirst().first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? rawTag) : rawTag
    let cleanTag = String(tagCandidate.prefix(4)).uppercased()
    let tag = cleanTag.isEmpty ? "8A21" : cleanTag
    return (name, tag)
}

// MARK: - Visual Mascot & Crescent Moon Components

struct WidgetFoxBadge: View {
    var size: CGFloat = 44

    var body: some View {
        ZStack {
            // Ambient outer aura
            Circle()
                .fill(RadialGradient(
                    colors: [Color.white.opacity(0.12), Color.white.opacity(0)],
                    center: .center,
                    startRadius: size * 0.1,
                    endRadius: size * 0.65
                ))
                .frame(width: size * 1.3, height: size * 1.3)

            // Stylized Glowing Fox Face
            Canvas { context, sz in
                let w = sz.width
                let h = sz.height
                let scale = min(w, h) / 80.0

                func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
                    CGPoint(x: (x - 40) * scale + w / 2, y: (y - 40) * scale + h / 2)
                }

                // Head outer contour
                var head = Path()
                head.move(to: pt(40, 24))
                head.addLine(to: pt(48, 15))
                head.addLine(to: pt(58, 26))
                head.addLine(to: pt(53, 38))
                head.addLine(to: pt(65, 48))
                head.addLine(to: pt(56, 52))
                head.addLine(to: pt(40, 66))
                head.addLine(to: pt(24, 52))
                head.addLine(to: pt(15, 48))
                head.addLine(to: pt(27, 38))
                head.addLine(to: pt(22, 26))
                head.addLine(to: pt(32, 15))
                head.closeSubpath()

                context.fill(head, with: .color(Color(red: 13/255, green: 13/255, blue: 16/255)))
                context.stroke(head, with: .color(.white), lineWidth: 1.8 * scale)

                // Left inner ear
                var leftEar = Path()
                leftEar.move(to: pt(33, 19))
                leftEar.addLine(to: pt(25, 27))
                leftEar.addLine(to: pt(32, 32))
                leftEar.closeSubpath()
                context.fill(leftEar, with: .color(Color.white.opacity(0.9)))

                // Right inner ear
                var rightEar = Path()
                rightEar.move(to: pt(47, 19))
                rightEar.addLine(to: pt(48, 32))
                rightEar.addLine(to: pt(55, 27))
                rightEar.closeSubpath()
                context.fill(rightEar, with: .color(Color.white.opacity(0.9)))

                // Left slanted glowing eye
                var leftEye = Path()
                leftEye.move(to: pt(29, 42))
                leftEye.addQuadCurve(to: pt(35, 43), control: pt(31, 40))
                leftEye.addQuadCurve(to: pt(29, 42), control: pt(32, 45))
                context.fill(leftEye, with: .color(.white))

                // Right slanted glowing eye
                var rightEye = Path()
                rightEye.move(to: pt(51, 42))
                rightEye.addQuadCurve(to: pt(45, 43), control: pt(49, 40))
                rightEye.addQuadCurve(to: pt(51, 42), control: pt(48, 45))
                context.fill(rightEye, with: .color(.white))

                // Muzzle & cheek contour
                var muzzle = Path()
                muzzle.move(to: pt(24, 51))
                muzzle.addLine(to: pt(31, 56))
                muzzle.addLine(to: pt(40, 65))
                muzzle.addLine(to: pt(49, 56))
                muzzle.addLine(to: pt(56, 51))
                context.stroke(muzzle, with: .color(.white), lineWidth: 1.4 * scale)

                // Nose
                var nose = Path()
                nose.move(to: pt(38.5, 58))
                nose.addLine(to: pt(41.5, 58))
                nose.addLine(to: pt(40, 60))
                nose.closeSubpath()
                context.fill(nose, with: .color(.white))
            }
            .frame(width: size, height: size)
        }
        .frame(width: size, height: size)
    }
}

struct WidgetMoonCrescent: View {
    var size: CGFloat = 30

    var body: some View {
        ZStack {
            // Subtle ambient halo
            Circle()
                .fill(RadialGradient(
                    colors: [Color.white.opacity(0.18), Color.white.opacity(0)],
                    center: .center,
                    startRadius: size * 0.15,
                    endRadius: size * 0.65
                ))
                .frame(width: size * 1.3, height: size * 1.3)

            Canvas { context, sz in
                let w = sz.width
                let h = sz.height
                let scale = min(w, h) / 64.0

                func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
                    CGPoint(x: (x - 32) * scale + w / 2, y: (y - 32) * scale + h / 2)
                }

                // Crescent curve
                var crescent = Path()
                crescent.move(to: pt(48, 16))
                crescent.addCurve(to: pt(31, 55), control1: pt(27, 29), control2: pt(27, 47))
                crescent.addCurve(to: pt(37, 8), control1: pt(15, 43), control2: pt(15, 19))
                crescent.addCurve(to: pt(48, 16), control1: pt(44, 12), control2: pt(42, 15))
                crescent.closeSubpath()

                context.fill(crescent, with: .color(Color(red: 240/255, green: 240/255, blue: 245/255)))

                // Crater textures
                var crater1 = Path()
                crater1.addEllipse(in: CGRect(x: (20 - 32) * scale + w / 2 - 3 * scale,
                                             y: (28 - 32) * scale + h / 2 - 3 * scale,
                                             width: 6 * scale, height: 6 * scale))
                context.fill(crater1, with: .color(Color(red: 184/255, green: 184/255, blue: 194/255)))

                var crater2 = Path()
                crater2.addEllipse(in: CGRect(x: (26 - 32) * scale + w / 2 - 2 * scale,
                                             y: (38 - 32) * scale + h / 2 - 2 * scale,
                                             width: 4 * scale, height: 4 * scale))
                context.fill(crater2, with: .color(Color(red: 184/255, green: 184/255, blue: 194/255)))
            }
            .frame(width: size, height: size)
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Timeline Provider
struct AfterClassWidgetTimelineProvider: TimelineProvider {
    typealias Entry = AfterClassWidgetEntry

    private let appGroupSuite = "group.org.example.annochat.widget"

    private func getSnapshotData() -> (
        onlineCount: Int,
        mostActiveRoom: String,
        anonName: String,
        publicId: String,
        showOnlineCount: Bool,
        showIdentity: Bool,
        showQuickMatch: Bool
    ) {
        let defaults = UserDefaults(suiteName: appGroupSuite) ?? UserDefaults.standard
        let onlineCount = defaults.integer(forKey: "online_count")
        let mostActive = defaults.string(forKey: "most_active_room") ?? "LateNight"
        let anonName = defaults.string(forKey: "user_anon_name") ?? "Moon Fox"
        let publicId = defaults.string(forKey: "user_public_id") ?? "8A21"
        let showCount = defaults.object(forKey: "show_online_count") as? Bool ?? true
        let showId = defaults.object(forKey: "show_identity") as? Bool ?? true
        let showMatch = defaults.object(forKey: "show_quick_match") as? Bool ?? true

        return (
            onlineCount: onlineCount > 0 ? onlineCount : 312,
            mostActiveRoom: mostActive,
            anonName: anonName,
            publicId: publicId,
            showOnlineCount: showCount,
            showIdentity: showId,
            showQuickMatch: showMatch
        )
    }

    func placeholder(in context: Context) -> AfterClassWidgetEntry {
        AfterClassWidgetEntry(
            date: Date(),
            quote: widgetVibeQuotes[0],
            onlineCount: 312,
            mostActiveRoom: "LateNight",
            anonName: "Moon Fox",
            publicId: "8A21",
            avatarSymbol: "🦊",
            showOnlineCount: true,
            showIdentity: true,
            showQuickMatch: true,
            activityBars: [0.25, 0.30, 0.45, 0.75, 1.00, 0.65, 0.40, 0.30, 0.35, 0.55, 0.80, 1.00, 0.70, 0.50]
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (AfterClassWidgetEntry) -> Void) {
        let data = getSnapshotData()
        let entry = AfterClassWidgetEntry(
            date: Date(),
            quote: widgetVibeQuotes[0],
            onlineCount: data.onlineCount,
            mostActiveRoom: data.mostActiveRoom,
            anonName: data.anonName,
            publicId: data.publicId,
            avatarSymbol: "🦊",
            showOnlineCount: data.showOnlineCount,
            showIdentity: data.showIdentity,
            showQuickMatch: data.showQuickMatch,
            activityBars: [0.25, 0.30, 0.45, 0.75, 1.00, 0.65, 0.40, 0.30, 0.35, 0.55, 0.80, 1.00, 0.70, 0.50]
        )
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<AfterClassWidgetEntry>) -> Void) {
        let data = getSnapshotData()
        var entries: [AfterClassWidgetEntry] = []
        let currentDate = Date()

        for minuteOffset in stride(from: 0, to: 120, by: 15) {
            let entryDate = Calendar.current.date(byAdding: .minute, value: minuteOffset, to: currentDate) ?? currentDate
            let quoteIndex = (minuteOffset / 15) % widgetVibeQuotes.count
            let entry = AfterClassWidgetEntry(
                date: entryDate,
                quote: widgetVibeQuotes[quoteIndex],
                onlineCount: data.onlineCount,
                mostActiveRoom: data.mostActiveRoom,
                anonName: data.anonName,
                publicId: data.publicId,
                avatarSymbol: "🦊",
                showOnlineCount: data.showOnlineCount,
                showIdentity: data.showIdentity,
                showQuickMatch: data.showQuickMatch,
                activityBars: [0.25, 0.30, 0.45, 0.75, 1.00, 0.65, 0.40, 0.30, 0.35, 0.55, 0.80, 1.00, 0.70, 0.50]
            )
            entries.append(entry)
        }

        let refreshDate = Calendar.current.date(byAdding: .minute, value: 30, to: currentDate) ?? currentDate
        let timeline = Timeline(entries: entries, policy: .after(refreshDate))
        completion(timeline)
    }
}

// MARK: - 1. Time & Vibe Widget (Small 2x2)
struct TimeVibeWidgetView: View {
    var entry: AfterClassWidgetEntry

    private var formattedTime: (time: String, ampm: String) {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm"
        let time = formatter.string(from: entry.date)
        formatter.dateFormat = "a"
        let ampm = formatter.string(from: entry.date)
        return (time, ampm)
    }

    var body: some View {
        ZStack {
            // Realistic Lunar Background Image
            Image("widget_moon_bg")
                .resizable()
                .aspectRatio(contentMode: .fill)

            VStack(alignment: .leading, spacing: 0) {
                // Top Row: Brand Header
                Text("AfterClass")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)

                Spacer()

                // Large Digital Clock
                HStack(alignment: .lastTextBaseline, spacing: 4) {
                    Text(formattedTime.time)
                        .font(.system(size: 30, weight: .bold))
                        .foregroundColor(.white)
                    Text(formattedTime.ampm)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(white: 0.75))
                }

                Spacer().frame(height: 3)

                // Curated Nocturnal Quote
                Text("“\(entry.quote)”")
                    .font(.system(size: 11, weight: .regular))
                    .foregroundColor(Color(white: 0.65))
                    .lineLimit(2)

                Spacer()

                // Bottom Capsule: [ 👥 312 online ]
                HStack(spacing: 5) {
                    Image(systemName: "person.2.fill")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.white)
                    Text(entry.showOnlineCount ? "\(entry.onlineCount)" : "312")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                    Text("online")
                        .font(.system(size: 11, weight: .regular))
                        .foregroundColor(Color(white: 0.55))
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(
                    Capsule()
                        .fill(Color(red: 24/255, green: 24/255, blue: 29/255))
                        .overlay(Capsule().stroke(Color(white: 0.16), lineWidth: 1))
                )
            }
            .padding(14)
        }
        .containerBackground(Color(red: 11/255, green: 11/255, blue: 13/255), for: .widget)
        .widgetURL(URL(string: "afterclass://widget?action=open"))
    }
}

struct TimeVibeWidget: Widget {
    let kind: String = "TimeVibeWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: AfterClassWidgetTimelineProvider()) { entry in
            TimeVibeWidgetView(entry: entry)
        }
        .configurationDisplayName("Time & Vibe")
        .description("Real-time nocturnal clock with lunar graphic and live campus count.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - 2. Streak & Motivation Widget (Medium / Large)
struct LiveActivityWidgetView: View {
    var entry: AfterClassWidgetEntry

    private let dayLabels = ["M", "T", "W", "T", "F", "S", "S"]

    var body: some View {
        GeometryReader { geometry in
            HStack(spacing: 8) {
                // ─────────────────────────────────────────────────────────────
                // LEFT CARD (72%): 10. STREAK / USAGE
                // ─────────────────────────────────────────────────────────────
                Link(destination: URL(string: "afterclass://profile")!) {
                    VStack(alignment: .leading, spacing: 0) {
                        // Top Header: AfterClass + Keep showing up.
                        HStack(alignment: .center) {
                            Text("AfterClass")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                            Spacer()
                            Text("Keep showing up.")
                                .font(.system(size: 11, weight: .regular))
                                .foregroundColor(Color(white: 0.55))
                        }

                        Spacer(minLength: 4)

                        // Flame Disc Badge + Streak Count
                        HStack(alignment: .center, spacing: 10) {
                            ZStack {
                                Circle()
                                    .fill(Color(red: 24/255, green: 24/255, blue: 28/255))
                                    .frame(width: 38, height: 38)
                                    .overlay(
                                        Circle()
                                            .stroke(Color.white.opacity(0.18), lineWidth: 1)
                                    )
                                Image(systemName: "flame.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(.white)
                            }

                            Text("5")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.white)

                            Text("day streak")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color(white: 0.72))
                        }

                        Spacer(minLength: 6)

                        // 7-Day Consistency Tracker (M T W T F S S)
                        HStack(spacing: 0) {
                            ForEach(0..<7, id: \.self) { index in
                                VStack(spacing: 4) {
                                    if index < 4 {
                                        Circle()
                                            .fill(Color.white)
                                            .frame(width: 12, height: 12)
                                    } else {
                                        Circle()
                                            .stroke(Color(white: 0.4), lineWidth: 1.5)
                                            .frame(width: 12, height: 12)
                                    }

                                    Text(dayLabels[index])
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(index < 4 ? Color(white: 0.75) : Color(white: 0.45))
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }

                        Spacer(minLength: 6)

                        // Bottom Subcard: Still anonymous. Still you.
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Still anonymous.")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                            Text("Still you.")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(Color(white: 0.8))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(Color(red: 24/255, green: 24/255, blue: 28/255))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.white.opacity(0.12), lineWidth: 1)
                                )
                        )
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                }
                .frame(width: (geometry.size.width - 8) * 0.72)

                // ─────────────────────────────────────────────────────────────
                // RIGHT CARD (28%): 09. MOTIVATION
                // ─────────────────────────────────────────────────────────────
                Link(destination: URL(string: "afterclass://rooms")!) {
                    ZStack {
                        Image("widget_motivation_bg")
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: (geometry.size.width - 8) * 0.28, height: geometry.size.height)
                            .clipped()

                        VStack(spacing: 0) {
                            Spacer()
                            Text("“Strangers\ntoday,\nideas\ntomorrow.”")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                                .shadow(color: .black.opacity(0.8), radius: 3, x: 0, y: 1)
                            Spacer()
                        }
                        .padding(.horizontal, 4)
                    }
                    .frame(width: (geometry.size.width - 8) * 0.28, height: geometry.size.height)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(Color.white.opacity(0.15), lineWidth: 1)
                    )
                }
                .frame(width: (geometry.size.width - 8) * 0.28)
            }
            .padding(10)
        }
        .containerBackground(Color(red: 11/255, green: 11/255, blue: 13/255), for: .widget)
    }
}

struct LiveActivityWidget: Widget {
    let kind: String = "LiveActivityWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: AfterClassWidgetTimelineProvider()) { entry in
            LiveActivityWidgetView(entry: entry)
        }
        .configurationDisplayName("Streak & Motivation")
        .description("Campus streak tracker with 7-day consistency and nocturnal motivation.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

// MARK: - 3. Your Identity Widget (Small 2x2)
struct IdentityWidgetView: View {
    var entry: AfterClassWidgetEntry

    var body: some View {
        ZStack {
            Image("widget_identity_bg")
                .resizable()
                .aspectRatio(contentMode: .fill)

            VStack(spacing: 0) {
                HStack {
                    Text("AfterClass")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Image(systemName: "sparkles")
                        .font(.system(size: 12))
                        .foregroundColor(.white)
                }

                Spacer()

                WidgetFoxBadge(size: 84)

                Spacer().frame(height: 2)

                let (cleanName, cleanTag) = cleanPersona(entry.anonName, entry.publicId)
                Text(entry.showIdentity ? cleanName : "Anonymous Persona")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(1)

                Text("#\(cleanTag)")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Color(white: 0.55))

                Spacer().frame(height: 6)
            }
            .padding(16)
        }
        .containerBackground(Color(red: 11/255, green: 11/255, blue: 13/255), for: .widget)
        .widgetURL(URL(string: "afterclass://profile"))
    }
}

struct IdentityWidget: Widget {
    let kind: String = "IdentityWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: AfterClassWidgetTimelineProvider()) { entry in
            IdentityWidgetView(entry: entry)
        }
        .configurationDisplayName("Your Identity")
        .description("Your current anonymous animal persona badge and encrypted tag.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Unified Widget Bundle
#if WIDGET_EXTENSION
@main
#endif
struct AfterClassWidgetBundle: WidgetBundle {
    var body: some Widget {
        TimeVibeWidget()
        LiveActivityWidget()
        IdentityWidget()
    }
}
