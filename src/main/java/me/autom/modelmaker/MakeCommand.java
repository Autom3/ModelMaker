package me.autom.modelmaker;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Autom
 */
public class MakeCommand implements CommandExecutor {

    private final ModelMaker plugin;
    private final Map<String, String> names;

    public MakeCommand(ModelMaker plugin) {
        this.plugin = plugin;
        this.names = new HashMap<>();
        populateNames();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

        if (!sender.hasPermission("modelmaker.make")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can perform this command!");
            return true;
        }

        Player player = (Player) sender;
        WorldEditPlugin worldEdit = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        Selection selection = worldEdit.getSelection(player);

        if (selection == null || !(selection instanceof CuboidSelection)) {
            player.sendMessage(ChatColor.RED + "Make a cuboid selection first!");
            return true;
        }

        CuboidSelection cube = (CuboidSelection) selection;

        if (cube.getWidth() > 48) {
            player.sendMessage(ChatColor.RED + "Your selection is too wide! (max: 48, x-axis)");
            return true;
        }
        if (cube.getHeight() > 48) {
            player.sendMessage(ChatColor.RED + "Your selection is too high! (max: 48, y-axis)");
            return true;
        }
        if (cube.getLength() > 48) {
            player.sendMessage(ChatColor.RED + "Your selection is too long! (max: 48, z-axis)");
            return true;
        }

        ModelBlock[][][] blocks = new ModelBlock[cube.getWidth()][cube.getHeight()][cube.getLength()];
        Map<ModelBlock, Integer> map = new HashMap<>();

        int i = 0;
        for (int x = 0; x < cube.getWidth(); x++) {
            for (int y = 0; y < cube.getHeight(); y++) {
                for (int z = 0; z < cube.getLength(); z++) {
                    Block block = cube.getMinimumPoint().add(x, y, z).getBlock();
                    if (!block.getType().equals(Material.AIR)) {
                        blocks[x][y][z] = new ModelBlock(block.getType(), block.getData(),
                                cube.getMinimumPoint().add(x, y + 1, z).getBlock().getType().isOccluding(),
                                cube.getMinimumPoint().add(x, y - 1, z).getBlock().getType().isOccluding(),
                                cube.getMinimumPoint().add(x, y, z - 1).getBlock().getType().isOccluding(),
                                cube.getMinimumPoint().add(x + 1, y, z).getBlock().getType().isOccluding(),
                                cube.getMinimumPoint().add(x, y, z + 1).getBlock().getType().isOccluding(),
                                cube.getMinimumPoint().add(x - 1, y, z).getBlock().getType().isOccluding());
                        map.put(blocks[x][y][z], i);
                        i++;
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        builder.append("{").append("\n");
        builder.append("\t\"__comment\": \"Model generated using the ModelMaker plugin by Autom\",").append("\n");
        builder.append("\t\"textures\": {").append("\n");
        boolean first = true;
        for (ModelBlock block : map.keySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            if (block.getMaterial().equals(Material.QUARTZ_BLOCK) || block.getMaterial().equals(Material.STEP)) {
                builder.append("\t\t\"").append(map.get(block)).append("_top\": \"blocks/")
                        .append(names.get(block + "_top") == null ? block + "_top" : names.get(block + "_top")).append("\"").append("\n");
                builder.append(",\t\t\"").append(map.get(block)).append("_side\": \"blocks/")
                        .append(names.get(block + "_side") == null ? block + "_side" : names.get(block + "_side")).append("\"").append("\n");
                builder.append(",\t\t\"").append(map.get(block)).append("_bottom\": \"blocks/")
                        .append(names.get(block + "_bottom") == null ? block + "_bottom" : names.get(block + "_bottom")).append("\"").append("\n");
            } else {
                builder.append("\t\t\"").append(map.get(block)).append("\": \"blocks/")
                        .append(names.get(block.toString()) == null ? block.toString() : names.get(block.toString())).append("\"").append("\n");
            }
        }

        builder.append("\t},").append("\n");
        builder.append("\t\"elements\": [").append("\n");

        first = true;
        for (int x = 0; x < cube.getWidth(); x++) {
            for (int y = 0; y < cube.getHeight(); y++) {
                for (int z = 0; z < cube.getLength(); z++) {
                    int xRef = (8 - cube.getWidth() / 2) + x;
                    int yRef = (8 - cube.getHeight() / 2) + y;
                    int zRef = (8 - cube.getLength() / 2) + z;
                    if (blocks[x][y][z] != null && (blocks[x][y][z].isExposed())) {
                        if (first) {
                            first = false;
                        } else {
                            builder.append(",");
                        }

                        builder.append("\t\t{").append("\n");
                        if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() >= 8) {
                            builder.append("\t\t\t\"from\": [").append(xRef).append(".0, ").append(yRef + 0.5).append(", ").append(zRef).append(".0],").append("\n");
                        } else {
                            builder.append("\t\t\t\"from\": [").append(xRef).append(".0, ").append(yRef).append(".0, ").append(zRef).append(".0],").append("\n");
                        }

                        if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() < 8) {
                            builder.append("\t\t\t\"to\": [").append(xRef + 1).append(".0, ").append(yRef + 0.5).append(", ").append(zRef + 1).append(".0],").append("\n");
                        } else {
                            builder.append("\t\t\t\"to\": [").append(xRef + 1).append(".0, ").append(yRef + 1).append(".0, ").append(zRef + 1).append(".0],").append("\n");
                        }

                        boolean ffirst = true;
                        builder.append("\t\t\t\"faces\": {").append("\n");
                        if (!blocks[x][y][z].isDown()) {
                            if (ffirst) {
                                ffirst = false;
                            } else {
                                builder.append(",");
                            }
                            builder.append("\t\t\t\t\"down\": { \"texture\": \"#").append(map.get(blocks[x][y][z]));
                            if (blocks[x][y][z].getMaterial().equals(Material.QUARTZ_BLOCK) || blocks[x][y][z].getMaterial().equals(Material.STEP)) {
                                builder.append("_bottom");
                            }
                            builder.append("\", \"uv\": [0, 0, 16, 16]}").append("\n");
                        }
                        if (!blocks[x][y][z].isUp()) {
                            if (ffirst) {
                                ffirst = false;
                            } else {
                                builder.append(",");
                            }
                            builder.append("\t\t\t\t\"up\": { \"texture\": \"#").append(map.get(blocks[x][y][z]));
                            if (blocks[x][y][z].getMaterial().equals(Material.QUARTZ_BLOCK) || blocks[x][y][z].getMaterial().equals(Material.STEP)) {
                                builder.append("_top");
                            }
                            builder.append("\", \"uv\": [0, 0, 16, 16]}").append("\n");
                        }
                        if (!blocks[x][y][z].isNorth()) {
                            if (ffirst) {
                                ffirst = false;
                            } else {
                                builder.append(",");
                            }
                            builder.append("\t\t\t\t\"north\": { \"texture\": \"#").append(map.get(blocks[x][y][z]));
                            if (blocks[x][y][z].getMaterial().equals(Material.QUARTZ_BLOCK) || blocks[x][y][z].getMaterial().equals(Material.STEP)) {
                                builder.append("_side");
                            }
                            builder.append("\", \"uv\": ");
                            if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() < 8) {
                                builder.append("[0, 0, 8, 8]");
                            } else if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() >= 8) {
                                builder.append("[8, 8, 16, 16]");
                            } else {
                                builder.append("[0, 0, 16, 16]");
                            }
                            builder.append("}").append("\n");
                        }
                        if (!blocks[x][y][z].isEast()) {
                            if (ffirst) {
                                ffirst = false;
                            } else {
                                builder.append(",");
                            }
                            builder.append("\t\t\t\t\"east\": { \"texture\": \"#").append(map.get(blocks[x][y][z]));
                            if (blocks[x][y][z].getMaterial().equals(Material.QUARTZ_BLOCK) || blocks[x][y][z].getMaterial().equals(Material.STEP)) {
                                builder.append("_side");
                            }
                            builder.append("\", \"uv\": ");
                            if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() < 8) {
                                builder.append("[0, 0, 8, 8]");
                            } else if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() >= 8) {
                                builder.append("[8, 8, 16, 16]");
                            } else {
                                builder.append("[0, 0, 16, 16]");
                            }
                            builder.append("}").append("\n");
                        }
                        if (!blocks[x][y][z].isSouth()) {
                            if (ffirst) {
                                ffirst = false;
                            } else {
                                builder.append(",");
                            }
                            builder.append("\t\t\t\t\"south\": { \"texture\": \"#").append(map.get(blocks[x][y][z]));
                            if (blocks[x][y][z].getMaterial().equals(Material.QUARTZ_BLOCK) || blocks[x][y][z].getMaterial().equals(Material.STEP)) {
                                builder.append("_side");
                            }
                            builder.append("\", \"uv\": ");
                            if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() < 8) {
                                builder.append("[0, 0, 8, 8]");
                            } else if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() >= 8) {
                                builder.append("[8, 8, 16, 16]");
                            } else {
                                builder.append("[0, 0, 16, 16]");
                            }
                            builder.append("}").append("\n");
                        }
                        if (!blocks[x][y][z].isWest()) {
                            if (!ffirst) {
                                builder.append(",");
                            }
                            builder.append("\t\t\t\t\"west\": { \"texture\": \"#").append(map.get(blocks[x][y][z]));
                            if (blocks[x][y][z].getMaterial().equals(Material.QUARTZ_BLOCK) || blocks[x][y][z].getMaterial().equals(Material.STEP)) {
                                builder.append("_side");
                            }
                            builder.append("\", \"uv\": ");
                            if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() < 8) {
                                builder.append("[0, 0, 8, 8]");
                            } else if ((blocks[x][y][z].getMaterial().equals(Material.STEP) || blocks[x][y][z].getMaterial().equals(Material.WOOD_STEP)) && blocks[x][y][z].getData() >= 8) {
                                builder.append("[8, 8, 16, 16]");
                            } else {
                                builder.append("[0, 0, 16, 16]");
                            }
                            builder.append("}").append("\n");
                        }
                        builder.append("\t\t\t}").append("\n");
                        builder.append("\t\t}").append("\n");
                    }
                }
            }
        }

        builder.append("\t]").append("\n");
        builder.append("}").append("\n");

        String fileName;
        if (args.length == 0) {
            fileName = "model";
        } else {
            fileName = args[0];
        }

        File file = new File(plugin.getDataFolder().getPath() + File.separator + fileName + ".json");
        try (FileWriter fileWriter = new FileWriter(file, false);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                PrintWriter writer = new PrintWriter(bufferedWriter)) {
            writer.print(builder.toString());

            player.sendMessage(ChatColor.GREEN + fileName + ".json has been saved in the ModelMaker plugin directory!");
        } catch (IOException ex) {
            Logger.getLogger(MakeCommand.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    private class ModelBlock {

        private final Material material;
        private final int data;
        private final boolean up;
        private final boolean down;
        private final boolean north;
        private final boolean east;
        private final boolean south;
        private final boolean west;

        public ModelBlock(Material material, int data, boolean up, boolean down, boolean north, boolean east, boolean south, boolean west) {
            this.material = material;
            this.data = data;
            this.up = up;
            this.down = down;
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
        }

        @Override
        public String toString() {
            return material.name().toLowerCase() + ":" + data;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.material);
            hash = 89 * hash + this.data;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ModelBlock other = (ModelBlock) obj;
            if (this.material != other.material) {
                return false;
            }
            if (this.data != other.data) {
                return false;
            }
            return true;
        }

        public Material getMaterial() {
            return material;
        }

        public int getData() {
            return data;
        }

        public boolean isUp() {
            return up;
        }

        public boolean isDown() {
            return down;
        }

        public boolean isNorth() {
            return north;
        }

        public boolean isEast() {
            return east;
        }

        public boolean isSouth() {
            return south;
        }

        public boolean isWest() {
            return west;
        }

        private boolean isExposed() {
            return !(up && down && north && east && south && west);
        }
    }

    private void populateNames() {
        for (String key : plugin.getConfig().getConfigurationSection("names").getKeys(false)) {
            names.put(key, plugin.getConfig().getString("names." + key));
        }
    }
}
