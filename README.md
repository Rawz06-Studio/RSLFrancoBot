# RSLFrancoBot

A Discord bot for generating Ocarina of Time Randomizer seeds with support for multiple generation modes including Franco (classic), S8 (classic), and RSL modes (S7, PoT, Beginner).

## Features

- **Multiple Seed Modes**:
  - **Franco (classic)**: Franco S5 with customisable settings
  - **S8 (classic)**: Standard Season 8 tournament settings
  - **S7 (RSL)**: Random Settings League Season 7
  - **PoT (RSL)**: Pot of Time Tournament
  - **Beginner (RSL)**: Beginner-friendly RSL mode

- **Interactive Discord UI**:
  - Button-based mode selection
  - Multi-select menus for Franco options
  - Random option selection for Franco mode
  - Clean message management (auto-cleanup after seed generation)

- **Clean Architecture**:
  - 3-layer separation (Engine/Bot/API)
  - Fully testable with mock implementations
  - Easy to extend and maintain

## Architecture

The bot follows Clean Architecture principles with three main layers:

```
┌─────────────────────────────────────────────────────────┐
│                    BOT LAYER (JDA)                      │
│  Discord Interactions, Handlers, Presenters, Adapters   │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                  ENGINE LAYER (Core)                     │
│  Use Cases, Domain Entities, Ports (Interfaces)         │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                   API LAYER (External)                   │
│  Randomizer API, Python Script Runner, Repositories     │
└─────────────────────────────────────────────────────────┘
```

For detailed architecture documentation, see [docs/ARCHI.md](docs/ARCHI.md).

## Prerequisites

- **Java 21** or higher
- **Python 3** (for RSL script execution)
- **Maven** (for building)
- **Discord Bot Token** (from Discord Developer Portal)
- **OoT Randomizer API Key** (optional, for production use)

## Configuration

### Discord Bot Setup

1. Create a bot at [Discord Developer Portal](https://discord.com/developers/applications)
2. Enable the following intents:
   - Server Members Intent
   - Message Content Intent
3. Invite the bot to your server with the necessary permissions

### Application Properties

Create `src/main/resources/application-dev.properties` for local development:

```properties
# Discord Configuration
app.discord.token=YOUR_DISCORD_BOT_TOKEN

# OoT Randomizer API Configuration
app.randomizer.api.url=https://ootrandomizer.com/api/v2/seed/create
app.randomizer.api.key=YOUR_API_KEY
app.randomizer.api.version.rsl=devRSL_8.2.52-132
app.randomizer.api.version.pot=devRSL_8.2.52-132
app.randomizer.api.version.beginner=devRSL_8.2.52-132
app.randomizer.api.version.standard=8.3.0
app.randomizer.api.version.franco=devRSL_8.2.52-132

# Python Script Configuration
app.python.command=python3
app.python.script.dir=plando-random-settings
app.python.script.name=RandomSettingsGenerator.py
app.python.weights.rsl=weights/rsl-custom.json
app.python.weights.pot=weights/pot-custom.json
app.python.weights.beginner=weights/beginner-custom.json
```

## Local Development

### 1. Clone the Repository

```bash
git clone https://github.com/RawZ06/RSLFrancoBot.git
cd RSLFrancoBot
```

### 2. Clone Python Script Dependency

```bash
git clone https://github.com/matthewkirby/plando-random-settings.git
cd plando-random-settings
git checkout 50813f8
cd ..
```

### 3. Copy Custom Weights

```bash
cp weights/*.json plando-random-settings/weights/
```

### 4. Build and Run

```bash
# Build the project
mvn clean package

# Run the bot
java -jar target/rslfrancobot-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Docker Deployment

### Build the Docker Image

```bash
docker build -t rslfrancobot:latest .
```

### Run with Docker

```bash
docker run -d \
  --name rslfrancobot \
  -e DISCORD_TOKEN="your_discord_token" \
  -e RANDOMIZER_API_KEY="your_api_key" \
  rslfrancobot:latest
```

### Run with Docker Compose

Create a `docker-compose.yml`:

```yaml
version: '3.8'

services:
  bot:
    build: .
    container_name: rslfrancobot
    environment:
      - DISCORD_TOKEN=${DISCORD_TOKEN}
      - RANDOMIZER_API_KEY=${RANDOMIZER_API_KEY}
    restart: unless-stopped
```

Then run:

```bash
docker-compose up -d
```

## Usage

1. Invite the bot to your Discord server
2. Use the command `/seed` to start seed generation
3. Select your preferred mode:
   - **Franco (classic)**: Choose custom options or random selection
   - **S8 (classic)**: Instant generation with S8 settings
   - **S7/PoT/Beginner (RSL)**: Instant generation with RSL modes

### Franco Mode Options

Franco mode allows you to:
- Select multiple custom options from a menu
- Click "Random Selection" to choose a random number of compatible options
- Click "Validate & Generate" to create your seed

### Seed Result

After generation, you'll receive:
- Seed URL (clickable link to ootrandomizer.com)
- Version used
- Spoiler log availability
- Generation username
- Selected settings (for Franco mode)

## Project Structure

```
RSLFrancoBot/
├── docs/                           # Documentation
│   ├── ARCHI.md                   # Architecture details
│   ├── ARCHITECTURE_DIAGRAM.md    # Architecture diagrams
│   ├── PYTHON_CONFIG.md           # Python script setup
│   └── ...
├── src/
│   └── main/
│       ├── java/
│       │   └── fr/rawz06/rslfrancobot/
│       │       ├── bot/           # Discord layer (JDA)
│       │       │   ├── handlers/  # Button/interaction handlers
│       │       │   ├── models/    # Discord abstractions
│       │       │   ├── presenters/# Message formatters
│       │       │   └── services/  # Bot services
│       │       ├── engine/        # Core business logic
│       │       │   ├── domain/    # Entities, ports
│       │       │   └── usecases/  # Use cases
│       │       └── api/           # External adapters
│       │           ├── python/    # Python script runner
│       │           ├── randomizer/# HTTP API client
│       │           └── repositories/# Data repositories
│       └── resources/
│           ├── data/              # Preset definitions
│           ├── weights/           # Custom RSL weights
│           └── application.properties
├── plando-random-settings/        # Python script (cloned)
├── weights/                       # Custom weight files
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Testing

The project includes mock implementations for testing without external dependencies:

```bash
# Run with mock implementations (default)
mvn spring-boot:run

# The mock will:
# - Simulate API calls with 5-second delay
# - Display full settings JSON in console
# - Generate fake seed URLs
```

To enable real API calls, set `app.randomizer.api.mode=http` in your `application.properties`.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Technologies

- **Java 21** - Modern Java with records, switch expressions
- **Spring Boot 3.5.6** - Framework and dependency injection
- **JDA 6.0.0** - Java Discord API
- **Python 3** - RSL settings generation
- **Maven** - Build tool
- **Docker** - Containerization

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [plando-random-settings](https://github.com/matthewkirby/plando-random-settings) by matthewkirby
- [OoT Randomizer](https://ootrandomizer.com/) team
- Random Settings League community

## Support

For issues, questions, or contributions, please open an issue on GitHub.
Thank
