package com.eatsleepcode.undergroundvillagebuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Field;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;

import java.io.*;
import org.json.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class UndergroundVillageBuilder extends PluginBase {
	private static final String pluginPath = "plugins/UndergroundVillage/";
	private static final String[] FILE_PATHS = {pluginPath + "layout.json", pluginPath + "layout.csv", pluginPath + "layout.xlsx"};


	@Override
	public void onEnable() {
		getLogger().info(TextFormat.GREEN + "UndergroundVillageBuilder Loaded!");
	}

	@Override
	public void onDisable() {
		getLogger().info(TextFormat.RED + "UndergroundVillageBuilder Disabled!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("undergroundvillage")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(TextFormat.RED + "This command can only be used by a player.");
				return true;
			}

			Player player = (Player) sender;
			int currentY = (int) player.getPosition().y;
			int targetY = 1;

			// Set default target Y to 20 if no argument is provided
			if (args.length > 0) {
				try {
					targetY = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					player.sendMessage(TextFormat.RED + "Usage: /undergroundvillage [depthCoordinate]");
					return true;
				}
			}

			// Ensure the target Y is below the player's current Y position
			if (targetY >= currentY) {
				player.sendMessage(TextFormat.RED + "Target depth must be below player's current position.");
				return true;
			}

			Level level = player.getLevel();
			Vector3 start = new Vector3(player.getFloorX(), player.getFloorY() - 1, player.getFloorZ());
		
			ensureDefaultLayoutFile();
			build(level, player, start, getPlayerDirection(player), targetY);

			player.sendMessage(TextFormat.GREEN + "Underground village built successfully.");
			return true;
		}
		return false;
	}



	private void ensureDefaultLayoutFile() {
		File dir = new File(pluginPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(pluginPath + "layout.xlsx");

		if (!file.exists()) {
			try (InputStream input = getClass().getResourceAsStream("/layout.xlsx");
				FileOutputStream output = new FileOutputStream(file)) {
				if (input != null) {
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = input.read(buffer)) != -1) {
						output.write(buffer, 0, bytesRead);
					}
				}
			} catch (IOException e) {
				getLogger().warning("Failed to create default layout file: " + e.getMessage());
			}
		}
	}

	public void build(Level level, Player player, Vector3 start, String direction, int depth) {
		int baseX = player.getFloorX();
		int baseY = player.getFloorY();
		int baseZ = player.getFloorZ();

		File file = findLayoutFile();
		if (file == null) {
			player.sendMessage("No structure file found.");
			return;
		}

		List<List<List<String>>> structure = parseStructureFile(player, file);
		if (structure == null) {
			player.sendMessage("Failed to parse structure file.");
			return;
		}

		for (int y = 0; y < structure.size(); y++) {
			for (int z = 0; z < structure.get(y).size(); z++) {
				for (int x = 0; x < structure.get(y).get(z).size(); x++) {
					Block blockToSet = getBlockWithAdjustments(player, structure.get(y).get(z).get(x), direction);
					level.setBlock(new Vector3(baseX + x, baseY + y, baseZ + z), blockToSet, true, true);
				}
			}
		}
	}



	private File findLayoutFile() {
		for (String path : FILE_PATHS) {
			File file = new File(path);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}



	private List<List<List<String>>> parseStructureFile(Player player, File file) {
		try {
			if (file.getName().endsWith(".json")) {
				return parseJson(file);
			} else if (file.getName().endsWith(".csv")) {
				return parseCsv(file);
			} else if (file.getName().endsWith(".xlsx")) {
				return parseXlsx(file);
			}
		} catch (Exception e) {
			player.sendMessage("Error parsing structure file: " + e.getMessage());
		}
		return null;
	}



	private List<List<List<String>>> parseJson(File file) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(file.getPath())));
		JSONArray jsonArray = new JSONArray(content);
		List<List<List<String>>> structure = new ArrayList<>();
		for (int y = 0; y < jsonArray.length(); y++) {
			JSONArray layer = jsonArray.getJSONArray(y);
			List<List<String>> layerList = new ArrayList<>();
			for (int z = 0; z < layer.length(); z++) {
				JSONArray row = layer.getJSONArray(z);
				List<String> rowList = new ArrayList<>();
				for (int x = 0; x < row.length(); x++) {
					rowList.add(row.getString(x));
				}
				layerList.add(rowList);
			}
			structure.add(layerList);
		}
		return structure;
	}



	private List<List<List<String>>> parseCsv(File file) throws IOException {
        List<List<List<String>>> structure = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            List<List<String>> layer = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    structure.add(layer);
                    layer = new ArrayList<>();
                } else {
                    layer.add(Arrays.asList(line.split(",")));
                }
            }
            if (!layer.isEmpty()) {
                structure.add(layer);
            }
        }
        return structure;
    }

    private List<List<List<String>>> parseXlsx(File file) throws IOException {
        List<List<List<String>>> structure = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            for (Sheet sheet : workbook) {
                List<List<String>> layer = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> rowList = new ArrayList<>();
                    for (Cell cell : row) {
                        rowList.add(cell.toString());
                    }
                    layer.add(rowList);
                }
                structure.add(layer);
            }
        }
        return structure;
    }



	private Block getBlockWithAdjustments(Player player, String blockName, String direction) {
		// This method deals with items that require meta, setDamage, or uncovered assets
		blockName = blockName.replace("Block.", "");
		int blockID = Block.AIR;
		int blockDamage = 0;
		switch (blockName) {
			case "BED.WHITE.HEAD": 
				blockID = 355;
				switch (direction) {
					case "north":
						blockDamage = 0;		
						break;
					case "west":
						blockDamage = 0;
						break;
					case "south": 
						blockDamage = 0;
						break;
					case "east":
						blockDamage = 0;
						break;
				}
				break;
			case "BED.WHITE.FOOT": 
				blockID = 355;
				switch (direction) {
					case "north":
						blockDamage = 8;
						break;
					case "west":
						blockDamage = 8;
						break;
					case "south": 
						blockDamage = 8;
						break;
					case "east":
						blockDamage = 8;
						break;
				}
				break;
			case "DIRT_PATH": 
				blockID = 198;
				break;
			case "DOOR.OAK.TOP": 
				blockID = 64;
				break;
			case "DOOR.OAK.BOTTOM": 
				blockID = 64 ;
				break;
			case "PLANKS.OAK": 
				blockID = 5;
				blockDamage = 0;
				break;
			case "PLANKS.SPRUCE": 
				blockID = 5;
				blockDamage = 1;
				break;
			case "PLANKS.BIRCH": 
				blockID = 5;
				blockDamage = 2;
				break;
			case "PLANKS.JUNGLE": 
				blockID = 5;
				blockDamage = 3;
				break;
			case "PLANKS.ACACIA": 
				blockID =  5;
				blockDamage = 4;
				break;
			case "PLANKS.DARK_OAK": 
				blockID = 5;
				blockDamage = 5;
				break;
			case "FARMLAND":
				blockID = Block.FARMLAND;
				blockDamage = 7;
				break;
			default: 	
				blockID = getBlockIdFromName(player, blockName);
				break;
			}

		Block blockToReturn = Block.get(blockID);
		blockToReturn.setDamage(blockDamage);
		return blockToReturn;
		
	}

	public static int getBlockIdFromName(Player player, String blockName) {
        try {
            Field field = Block.class.getField(blockName.toUpperCase());
            return field.getInt(null);  
        } catch (NoSuchFieldException | IllegalAccessException e) {
            player.sendMessage(TextFormat.YELLOW + "Can not find the ID for " + blockName + ".");
            return Block.AIR; 
        }
    }


	private Vector3 transformCoordinates(Vector3 start, int x, int z, int y, String direction) {
		// Adjust coordinates based on player direction
		switch (direction) {
			case "north": 
				return new Vector3(start.x + x, y, start.z + z);
			case "east":
				return new Vector3(start.x + z, y, start.z + x);
			case "west":
				return new Vector3(start.x - z, y, start.z + x);
			default:
				return new Vector3(start.x + x, y, start.z + z); // Default to south
		}
	}



	private String getPlayerDirection(Player player) {
		float yaw = (float) player.getYaw(); // Cast to float for Nukkit
		if (yaw < 0) {
			yaw += 360;
		}
		yaw %= 360;

		if (yaw >= 315 || yaw < 45) {
			return "south";
		} else if (yaw >= 45 && yaw < 135) {
			return "west";
		} else if (yaw >= 135 && yaw < 225) {
			return "north";
		} else {
			return "east";
		}
	}
}

