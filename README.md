# Anvil Insight

Anvil Insight is a client-side Fabric mod for Minecraft 26.3. It adds a small information button to the vanilla anvil screen. The expandable panel explains the synchronized vanilla level cost, enchantment changes and conflicts, repair work, renaming, prior-work penalties, and the survival cost limit without changing any anvil mechanics.

## Development

- Minecraft: 26.3
- Fabric Loader: 0.19.5
- Fabric API: 0.160.7+26.3
- Fabric Loom: 1.17.21
- Gradle: 9.6.0
- Mod version: 1.1+mc26.3
- Java release: 25
- Mappings: no external mappings dependency; Minecraft 26.3 exposes the Mojang-named classes used by Loom directly

Use a Java 25 or newer JDK and run `./gradlew build`. `./gradlew runClient` launches the development client. The release JAR is `build/libs/anvil-insight-1.1+mc26.3.jar`; the `-sources.jar` is for development only.

## Design

The total shown by the panel always comes from `AnvilMenu#getCost`, the same synchronized value used by the vanilla screen. Detailed rows mirror Minecraft 26.3's `AnvilMenu#createResult` algorithm. If the mirrored breakdown and vanilla total ever disagree during a transient synchronization state, detailed numeric rows are withheld instead of presenting an approximation.

The mod uses Fabric screen events for the button and rendering. Its only mod-owned Mixin is an accessor for the vanilla container origin, used to keep both panels on-screen in narrow windows. It has no custom networking and is marked client-only.

## Validation

The migration was built with Gradle 9.6.0 on Temurin 26.0.2.1, targeting Java 25 bytecode. There are no automated test sources; Gradle reports `test NO-SOURCE`. The development client reached the Minecraft 26.3 title screen, loaded Anvil Insight, and exited cleanly. Mixin export confirmed all seven container accessor methods were applied without transformation errors.

The vanilla anvil menu's disassembled bytecode was identical before and after the migration, so the existing cost calculations did not require changes. Manual in-game validation remains for repairs, enchantment combinations and conflicts, renaming, prior-work costs, the 39/40-level boundary, Creative mode, panel scrolling, tooltips, and narrow-window layout. Development-account authentication failures for online services do not prevent local startup.

## License

This project is available under the CC0 license.
