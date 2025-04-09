package me.evade.chunkBuster;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChunkBuster extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private Set<UUID> activeBusters = new HashSet<>();
    private Set<Chunk> activeChunks = new HashSet<>();
    @Override
    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("chunkbuster").setExecutor(new ChunkBusterCommand());
        getCommand("cb").setExecutor(new ChunkBusterCommand());
        getCommand("chunkbuster").setTabCompleter(new ChunkBusterTabCompleter());
        getCommand("cb").setTabCompleter(new ChunkBusterTabCompleter());
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                config = new YamlConfiguration();
                config.set("destruction-delay-seconds", 5);
                config.save(configFile);
            } catch (IOException e) {
                getLogger().severe("Could not create config file: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    private class ChunkBusterCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length != 3 || !args[0].equalsIgnoreCase("give")) {
                sender.sendMessage(ChatColor.RED + "Usage: /chunkbuster give <player> <amount>");
                return true;
            }
            if (!sender.hasPermission("chunkbuster.give")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
                return true;
            }
            giveChunkBuster(target, amount);
            sender.sendMessage(ChatColor.GREEN + "Given " + amount + " Chunk Buster(s) to " + target.getName());
            return true;
        }
    }

    private class ChunkBusterTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                if (sender.hasPermission("chunkbuster.give")) {
                    completions.add("give");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("chunkbuster.give")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("chunkbuster.give")) {
                completions.addAll(Arrays.asList("1", "16", "32", "64"));
            }
            return completions;
        }
    }

    private void giveChunkBuster(Player player, int amount) {
        ItemStack item = new ItemStack(Material.BEACON, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Chunk Buster");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Place the chunk buster in a chunk to destroy all blocks in it until you hit bedrock!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.BEACON && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Chunk Buster")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (!player.hasPermission("chunkbuster.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use Chunk Busters.");
                return;
            }
            if (activeBusters.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You already have an active Chunk Buster. Please wait until it finishes.");
                return;
            }
            ItemStack handItem = player.getInventory().getItemInMainHand();
            handItem.setAmount(handItem.getAmount() - 1);
            Block placedBlock = event.getBlockPlaced();
            Chunk chunk = placedBlock.getChunk();
            activeBusters.add(player.getUniqueId());
            activeChunks.add(chunk);
            player.sendMessage(ChatColor.GREEN + "Chunk Buster activated! Breaking blocks...");
            int destructionDelay = config.getInt("destruction-delay-seconds", 5);
            new ChunkDestroyer(player, chunk, placedBlock).runTaskTimer(this, 0, destructionDelay);
        }
    }
    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        if (activeChunks.contains(event.getBlock().getChunk()) || activeChunks.contains(event.getToBlock().getChunk())) {
            Material type = event.getBlock().getType();
            if (type == Material.WATER || type == Material.LAVA ||
                    type.name().contains("WATER") || type.name().contains("LAVA")) {
                event.setCancelled(true);
            }
        }
    }

    private class ChunkDestroyer extends BukkitRunnable {
        private Player player;
        private Chunk chunk;
        private Block initialBlock;
        private int minY;
        private int currentX;
        private int currentZ;
        public ChunkDestroyer(Player player, Chunk chunk, Block initialBlock) {
            this.player = player;
            this.chunk = chunk;
            this.initialBlock = initialBlock;
            this.minY = chunk.getWorld().getMinHeight();
            this.currentX = 0;
            this.currentZ = 0;
        }
        @Override
        public void run() {
            if (currentX >= 16) {
                player.sendMessage(ChatColor.GREEN + "Chunk destruction complete!");
                activeBusters.remove(player.getUniqueId());
                activeChunks.remove(chunk);
                cancel();
                return;
            }
            destroyColumn(currentX, currentZ);
            currentZ++;
            if (currentZ >= 16) {
                currentZ = 0;
                currentX++;
                preventLiquidFlow();
            }
        }
        private void destroyColumn(int x, int z) {
            int maxY = chunk.getWorld().getMaxHeight() - 1;
            for (int y = maxY; y >= minY; y--) {
                Block block = chunk.getBlock(x, y, z);
                if (block.getType() != Material.BEDROCK) {
                    block.setType(Material.AIR);
                }
            }
        }
        private void preventLiquidFlow() {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getWorld().getMaxHeight() - 1; y >= minY; y--) {
                        Block block = chunk.getBlock(x, y, z);
                        Material type = block.getType();
                        if ((type == Material.WATER || type == Material.LAVA ||
                                type.name().contains("WATER") || type.name().contains("LAVA")) &&
                                (block.getData() != 0)) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }
}