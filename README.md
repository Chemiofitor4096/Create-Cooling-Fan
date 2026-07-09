# Create Cooling Fan

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-blue.svg)](https://www.minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.4.0-red.svg)](https://files.minecraftforge.net)
[![Create](https://img.shields.io/badge/Create-0.5.1.j-orange.svg)](https://modrinth.com/mod/create)

A Minecraft mod that bridges [Create](https://github.com/Creators-of-Create/Create) and [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct), letting encased fans interact with the casting process.

## ✨ Features

- **Fan-Accelerated Casting** — Create's encased fan airflow affects TiC casting tables/basins, speeding up (or slowing down) the solidification process based on fan speed, direction, and processing type.
- **Per-Type Configurable Factors** — Each `FanProcessingType` (Splashing, Haunting, Smoking, Blasting, and any mod-added types) has its own configurable factor. Water cools fast, lava slows it down.
- **Extensible API** — Block entities implement `IFanProcessingTarget` to receive custom fan processing logic. Other mods can use this API to make their own blocks react to Create's airflow.

## 📦 Dependencies

| Mod | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.0+ |
| Create | 0.5.1.j+ |
| Tinkers' Construct | 3.10.1.0+ |
| Mantle | 1.11.81+ |

## 🔧 How It Works

### Fan Processing

When a Create encased fan blows air over a TiC casting table or basin, the `CastingContainerProcessing` mixin intercepts the casting timer and adjusts it:

- **Blowing (positive speed)** — advances the casting timer, reducing solidification time.
- **Sucking (negative speed)** — reverses the timer, extending solidification time.
- **Processing type matters** — blowing through water (Splashing) cools fastest; blowing through lava (Blasting) heats the cast and slows cooling.

The effect strength scales with: `sqrt(|speed| / 64) × factor`, with the factor configurable per processing type.

### Configuration

Two config files are generated on first launch:

**`config/createcoolingfan-server.toml`** — Forge-managed, contains the fallback default:
```toml
[Casting Fan Factors]
    defaultFactor = 0.5   # range: -5.0 ~ 5.0
```

**`config/createcoolingfan-factors.json`** — auto-managed per-type factors, auto-populated on startup:
```json
{
    "create:splashing": 1.0,
    "create:haunting": 0.5,
    "create:smoking": -0.3,
    "create:blasting": -0.5
}
```

If another mod registers a new `FanProcessingType` (e.g. `somemod:freezing`), it will automatically appear in the JSON on next startup — no warnings, no manual steps.

### API

The `IFanProcessingTarget` interface allows any block entity to respond to Create's fan airflow:

```java
public interface IFanProcessingTarget {
    // Called each tick while the fan is affecting this block
    void ccf$process(FanProcessingType processingType, float speed);

    // Return true if this target can be processed by the given type
    boolean ccf$canProcess(FanProcessingType processingType);
}
```

Implement this interface on your block entity and it will automatically receive fan processing events via the `InjectAirCurrent` mixin.

## 📦 Using as a Dependency

### Maven

```groovy
repositories {
    maven { url = "https://maven.kessokuteatime.work/releases" }
}

dependencies {
    modImplementation "com.chemiofitor:createcoolingfan:1.0"
}
```
## 🏗️ Building

```bash
./gradlew build
```

The compiled jar will be at `build/libs/createcoolingfan-1.0.jar`.

To publish to your local Maven repository:
```bash
./gradlew publishMavenJavaPublicationToMavenLocal
```

## 📁 Project Structure

```
src/main/java/com/chemiofitor/createcoolingfan/
├── CreateCoolingFan.java              # Main mod class
├── api/
│   └── IFanProcessingTarget.java      # API interface for fan-processable blocks
├── config/
│   └── CCFConfig.java                 # Config system with auto-discovery
└── mixin/
    ├── InjectAirCurrent.java          # Mixin into Create's AirCurrent
    └── CastingContainerProcessing.java  # Mixin into TiC's CastingBlockEntity
```

## 📄 License

MIT — see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Chemiofitor** ([GitHub](https://github.com/Chemiofitor4096))

---

*Built with [Create](https://github.com/Creators-of-Create/Create) and [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct).*
