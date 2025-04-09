# ChunkBuster

A Minecraft Spigot/Paper plugin that allows players to instantly destroy an entire chunk down to bedrock with the placement of a special item.

## Features

- **Chunk Destruction**: Place a Chunk Buster to destroy all blocks in a chunk down to bedrock
- **Permission-Based Usage**: Control who can use and distribute Chunk Busters
- **Liquid Flow Prevention**: Automatically prevents liquid from flowing during chunk destruction
- **Configurable Delay**: Set how quickly chunks are destroyed

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/chunkbuster give <player> <amount>` | `chunkbuster.give` | Give a player Chunk Busters |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `chunkbuster.command` | Access to base command | true |
| `chunkbuster.give` | Ability to give Chunk Busters | op |
| `chunkbuster.use` | Ability to use Chunk Busters | op |

## How It Works

1. Players with the correct permission can receive a Chunk Buster beacon
2. Placing the beacon activates the Chunk Buster (the beacon isn't actually placed)
3. The entire chunk begins to be destroyed column by column
4. Liquid flow is prevented during the destruction process
5. Only bedrock remains after the process is complete
6. Players can only have one active Chunk Buster at a time

## Configuration

```yaml
# The delay in seconds between destroying each column
destruction-delay-seconds: 5
```

## Usage

To use a Chunk Buster:

1. Get a Chunk Buster from an admin using `/cb give <player> <amount>`
3. Right-click to place it in the chunk you want to destroy
4. Wait for the destruction to complete
5. Each use consumes one Chunk Buster from your inventory

## Installation

1. Download the ChunkBuster.jar file
2. Place it in your server's plugins folder
3. Restart your server
4. Configuration will be automatically generated at plugins/ChunkBuster/config.yml
