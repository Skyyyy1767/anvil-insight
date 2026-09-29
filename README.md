# Anvil Insight

Anvil Insight is a client-side mod for Minecraft 26.3. It adds a small information button to the vanilla anvil screen. The expandable panel explains the synchronized vanilla level cost, enchantment changes and conflicts, repair work, renaming, prior-work penalties, and the survival cost limit without changing any anvil mechanics.

Anvil Insight supports Fabric, Quilt, NeoForge, and Forge. Install the jar whose loader name matches the instance.

## Development

- Minecraft: 26.3
- Mod version: 1.1+mc26.3
- Java release: 25
- Gradle: 9.6.0
- Fabric Loader: 0.19.5
- Quilt Loader: 0.31.0-beta.4
- NeoForge: 26.3.0.33-beta
- Forge: 26.3-66.0.8

Use a Java 25 or newer JDK. Build every distribution with:

```shell
./gradlew build
```

Individual loader builds are available as `:fabric:build`, `:quilt:build`, `:neoforge:build`, and `:forge:build`. Distributable jars are written to each loader module's `build/libs` directory:

- `fabric/build/libs/anvil-insight-fabric-1.1+mc26.3.jar`
- `quilt/build/libs/anvil-insight-quilt-1.1+mc26.3.jar`
- `neoforge/build/libs/anvil-insight-neoforge-1.1+mc26.3.jar`
- `forge/build/libs/anvil-insight-forge-1.1+mc26.3.jar`

The `-sources.jar` files are for development only.

IntelliJ IDEA imports four shared Run/Debug configurations: `Fabric Client`, `Quilt Client`, `NeoForge Client`, and `Forge Client`. Each runs from an isolated directory under `run/<loader>`. The equivalent root Gradle tasks are `runFabricClient`, `runQuiltClient`, `runNeoForgeClient`, and `runForgeClient`.

## Project structure

`common` is a source-only shared module. It contains the anvil analysis, vanilla-style panel and controller, translations, icon, and client mixins. Every loader compiles those same sources into its own distributable jar, so feature changes do not need to be copied between loaders.

`fabric`, `quilt`, `neoforge`, and `forge` contain only their build configuration, loader metadata, and any required loader entry class. Fabric and Quilt need no entry class because the shared mixins attach the interface directly to the vanilla anvil screen. NeoForge and Forge contain minimal loader discovery classes.

Dependency and project versions are centralized in `gradle.properties`. Fabric Loom builds both the Fabric and Quilt artifacts because Minecraft 26.3 is available with the official class names while Quilt Loom does not publish 26.3 mappings. The Quilt artifact still targets Quilt Loader and contains Quilt metadata only. NeoForge uses ModDevGradle, and Forge uses ForgeGradle.

Forge-family mod IDs cannot contain hyphens, so NeoForge and Forge use `anvil_insight`; Fabric and Quilt retain the existing `anvil-insight` ID.

## Design

The total shown by the panel always comes from `AnvilMenu#getCost`, the same synchronized value used by the vanilla screen. Detailed rows mirror Minecraft 26.3's `AnvilMenu#createResult` algorithm. If the mirrored breakdown and vanilla total ever disagree during a transient synchronization state, detailed numeric rows are withheld instead of presenting an approximation.

Shared client mixins connect the controller to `AnvilScreen` and route rendering, clicks, dragging, releases, and mouse-wheel scrolling through the existing panel behavior. Loader APIs do not appear in the UI or analysis code.

The project compiles and runs on Java 25, while the shared Mixin configuration declares `JAVA_21`, the highest compatibility level recognized by both Fabric's Mixin fork and Forge's upstream Mixin 0.8.7 runtime. The shared mixins do not require language features above that level.

## Validation

All four loader distributions compile and build from the root Gradle build. The built jars are checked for their loader-specific metadata, the shared mixin configuration, the shared UI classes, and loader-specific filenames. No Minecraft client launch is required for routine build validation.

## License

This project is available under the CC0 license.
