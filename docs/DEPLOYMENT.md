# Deployment Guide

This guide explains how to deploy RSLFrancoBot using Docker.

## Prerequisites

- Docker installed and running
- Discord Bot Token (from [Discord Developer Portal](https://discord.com/developers/applications))
- OoT Randomizer API Key (optional for testing with Mock)

## Quick Start with Docker Compose

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/RSLFrancoBot.git
cd RSLFrancoBot
```

### 2. Create Environment File

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your values:

```bash
# Discord Bot Configuration
DISCORD_TOKEN=your_discord_bot_token_here

# OoT Randomizer API Configuration
RANDOMIZER_API_KEY=your_randomizer_api_key_here

# Java Options (optional)
JAVA_OPTS=-Xmx512m -Xms256m
```

### 3. Build and Run

```bash
# Build and start the bot
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the bot
docker-compose down
```

## Manual Docker Build

### Build the Image

```bash
docker build -t rslfrancobot:latest .
```

### Run the Container

```bash
docker run -d \
  --name rslfrancobot \
  -e DISCORD_TOKEN="your_discord_token" \
  -e RANDOMIZER_API_KEY="your_api_key" \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  --restart unless-stopped \
  rslfrancobot:latest
```

### View Logs

```bash
# Follow logs
docker logs -f rslfrancobot

# View last 100 lines
docker logs --tail 100 rslfrancobot
```

### Stop/Restart

```bash
# Stop
docker stop rslfrancobot

# Start
docker start rslfrancobot

# Restart
docker restart rslfrancobot

# Remove
docker rm -f rslfrancobot
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DISCORD_TOKEN` | Yes | - | Discord bot token |
| `RANDOMIZER_API_KEY` | No* | - | OoT Randomizer API key |
| `JAVA_OPTS` | No | `-Xmx512m -Xms256m` | JVM options |
| `SPRING_PROFILES_ACTIVE` | No | `prod` | Spring profile |
| `APP_RANDOMIZER_API_MODE` | No | `http` | API Mode (`http` or `mock`) |

\* Required for production use. Mock implementation works without it.

## Using Mock vs Production

### Mock Mode (Default)

The bot uses mock implementations by default for testing:
- No real API calls
- 5-second simulated delay
- Displays full settings JSON in logs
- Perfect for local development

To use Mock mode, set `app.randomizer.api.mode=mock` in your properties or via environment variables.

### Production Mode

To enable real API calls:

1. Ensure you have a valid `RANDOMIZER_API_KEY`
2. Set `app.randomizer.api.mode=http` in `application.properties` or set environment variable `APP_RANDOMIZER_API_MODE=http`.

3. Rebuild and redeploy:

```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Troubleshooting

### Bot doesn't start

Check logs for errors:
```bash
docker logs rslfrancobot
```

Common issues:
- Missing `DISCORD_TOKEN`
- Invalid Discord token
- Network connectivity issues

### Python script errors

The Python script (`plando-random-settings`) is cloned during Docker build at commit `50813f8`.

If you see Python errors:
1. Ensure custom weights are in `weights/` directory
2. Rebuild the image: `docker-compose build --no-cache`

### Memory issues

If the bot crashes due to memory:
1. Increase JVM heap size via `JAVA_OPTS`
2. Adjust Docker container memory limits

```yaml
services:
  rslfrancobot:
    # ... other config ...
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
```

### Discord API Rate Limits

The bot is designed to clean up messages after seed generation to avoid clutter. If you experience rate limits:
- Reduce concurrent seed generations
- Check for multiple bot instances running

## Production Recommendations

### Security

1. **Never commit sensitive data**:
   - Add `.env` to `.gitignore`
   - Use Docker secrets or environment variables

2. **Run as non-root user**:
   - The Dockerfile already creates and uses `botuser`

3. **Keep dependencies updated**:
   ```bash
   # Update base images
   docker-compose pull
   docker-compose build --no-cache
   docker-compose up -d
   ```

### Monitoring

1. **Health checks**: The docker-compose includes health checks

2. **Log rotation**: Configured in docker-compose.yml:
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

3. **Resource limits**: Set appropriate memory limits based on usage

### Backup

Important files to backup:
- `.env` file (securely)
- Custom `weights/*.json` files
- Any modified presets in `src/main/resources/data/`

## Updating the Bot

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# Verify
docker-compose logs -f
```

## Multi-Server Deployment

To run multiple instances for different servers:

```yaml
version: '3.8'

services:
  bot-server1:
    build: .
    container_name: rslfrancobot-server1
    environment:
      - DISCORD_TOKEN=${DISCORD_TOKEN_SERVER1}
      - RANDOMIZER_API_KEY=${RANDOMIZER_API_KEY}
    restart: unless-stopped

  bot-server2:
    build: .
    container_name: rslfrancobot-server2
    environment:
      - DISCORD_TOKEN=${DISCORD_TOKEN_SERVER2}
      - RANDOMIZER_API_KEY=${RANDOMIZER_API_KEY}
    restart: unless-stopped
```

## Support

For issues or questions:
- Check logs: `docker logs rslfrancobot`
- Open an issue on GitHub
- Review [Architecture Documentation](ARCHI.md)
