# RSLFrancoBot Refactoring Summary

## Overview
Complete refactoring to externalize all seed generation to an external API. The bot is now Discord-focused only, with Franco mode as the primary local feature.

## Changes

### 🔄 API Integration
- **Removed:** Direct calls to ootrandomizer.com
- **New:** External seed generation API at `{app.seed.api.domain}/api/seed/{SeedMode}`
- **Pattern:** Franco parameters are passed as query parameters (e.g., `?boss_key=true`)

### 📁 Directory Removals
```
plando-random-settings/     # Python seed generation
weights/                    # RSL weight files
src/main/java/.../usecases/rsl/
src/main/java/.../usecases/s8/
src/main/java/.../usecases/s9/
src/main/java/.../usecases/tot/
src/main/java/.../usecases/allsanity/
src/main/java/.../usecases/mixed/
src/main/java/.../usecases/salad/
src/main/java/.../bot/handlers/rsl/
src/main/java/.../bot/handlers/salad/
src/main/java/.../bot/handlers/std/
src/main/java/.../bot/handlers/allsanity/
```

### 📄 Configuration Changes
**Before:** Multiple API version configurations
```properties
app.randomizer.api.url=https://ootrandomizer.com/api/v2/seed/create
app.randomizer.api.key=...
app.randomizer.api.version.rsl=...
app.randomizer.api.version.s8=...
# ... many more version configs
```

**After:** Single external API domain
```properties
app.seed.api.domain=http://localhost:8081
app.seed.api.mode=http|mock
```

### 🐳 Docker Optimization
**Before:**
- Python 3.14 + uv
- plando-random-settings repo
- Weight files
- Multi-layer complexity

**After:**
- Java only
- Minimal Alpine Linux image
- Clean and focused on Discord integration

### 🔧 Use Cases
**Kept (Franco only):**
- `GenerateFrancoSeedUseCase` - Simplified to direct API call
- `ValidateSettingsUseCase` - Local validation for Discord
- `BuildFinalSettingsUseCase` - Local settings construction

**New:**
- `GenerateExternalSeedUseCase` - Generic API call for non-Franco modes

**Removed:**
- All RSL, S8, S9, Salad, Allsanity, Mixed use cases

### 🎯 Generator Versions (Config)
New `GeneratorVersionsConfig` provides a centralized map of SeedMode → version:
```
S8: 8.3.0
S9: 9.0.0
ToT: 9.0.0
Mixed Pool: devFenhl_9.0.42-2
Franco: devrreal_9.0.2-17
RSL: devRSL_9.0.2-17
Enemy Salad: devEnemyShuffle_9.0.2-24
Salad: devrreal_9.0.2-17
Allsanity: devEnemyShuffle_9.0.2-24
```

### 📦 Data Files
**Kept:**
- `franco.yaml` - Franco mode options/parameters

**Removed:**
- franco.json
- s8.json, s9.json, tot.json
- salad.json
- allsanity.json
- mixed.json

### 🔗 Discord Bot
**Updated:**
- `JDAEventListener` - Only Franco handlers and base commands
- `SeedCommandHandler` - Routes to Franco/external API
- `AllCommandHandler` - Unchanged
- `InfoCommandHandler` - Unchanged

**Removed handlers:**
- RSL, PoT, Beginner, RoT button handlers
- S8, S9, ToT, Mixed button handlers
- Salad (all variants) button handlers
- Allsanity (all variants) button handlers

### API Layer
**Updated:**
- `HttpRandomizerApiAdapter` - Now calls external seed API via GET
- `MockRandomizerApiAdapter` - Simulates external API responses
- `RandomizerApiService` - Simplified for external API

**Pattern:** URL construction with query parameters for Franco, standard headers for others

### Dependencies
**New:**
- `jackson-dataformat-yaml:2.22.2` - For parsing franco.yaml

**Removed (via Dockerfile):**
- Python 3.14
- uv package manager
- git clone of plando-random-settings
- Custom weight files

## Migration Path for External API

When implementing the external seed generation API, use this specification:

### Endpoint: `GET /api/seed/{SeedMode}`
**Franco Example:**
```
GET /api/seed/FRANCO?boss_key=true&shuffle_ocarina=true
```

**Response (200 OK):**
```json
{
  "seedUrl": "https://ootrandomizer.com/seed/get?id=ABC123",
  "version": "devrreal_9.0.2-17",
  "spoilers": true,
  "usedSettings": { ... }
}
```

### Query Parameters
- Each setting from Franco preset becomes a query parameter
- Boolean values: `true`/`false`
- All other types: stringified

## Files Created
- `GeneratorVersionsConfig.java` - Centralized version management
- `GenerateExternalSeedUseCase.java` - Generic external API handling
- `FrancoYamlPresetRepository.java` - Franco YAML configuration loader

## Testing
- ✅ Compilation: `mvn clean compile -DskipTests`
- ✅ Package: `mvn package -DskipTests`
- Docker build ready (removed Python/plando-random-settings dependencies)

## Environment Variables
**Production:**
```bash
DISCORD_TOKEN=your_token_here
SEED_API_DOMAIN=https://your-api.example.com
SPRING_PROFILES_ACTIVE=prod
```

**Development:**
```bash
DISCORD_TOKEN=your_token_here
SEED_API_DOMAIN=http://localhost:8081
SPRING_PROFILES_ACTIVE=dev
```
