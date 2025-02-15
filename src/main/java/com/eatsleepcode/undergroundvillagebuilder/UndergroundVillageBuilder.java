package com.eatsleepcode.undergroundvillagebuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.lang.reflect.Field;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.passive.EntityAnimal;
import cn.nukkit.entity.passive.EntityCat;
import cn.nukkit.entity.passive.EntityChicken;
import cn.nukkit.entity.passive.EntityCod;
import cn.nukkit.entity.passive.EntityCow;
import cn.nukkit.entity.passive.EntityDolphin;
import cn.nukkit.entity.passive.EntityFish;
import cn.nukkit.entity.passive.EntityFox;
import cn.nukkit.entity.passive.EntityHorse;
import cn.nukkit.entity.passive.EntityOcelot;
import cn.nukkit.entity.passive.EntityPanda;
import cn.nukkit.entity.passive.EntityParrot;
import cn.nukkit.entity.passive.EntityPig;
import cn.nukkit.entity.passive.EntityPolarBear;
import cn.nukkit.entity.passive.EntityPufferfish;
import cn.nukkit.entity.passive.EntityRabbit;
import cn.nukkit.entity.passive.EntitySalmon;
import cn.nukkit.entity.passive.EntitySheep;
import cn.nukkit.entity.passive.EntityTropicalFish;
import cn.nukkit.entity.passive.EntityTurtle;
import cn.nukkit.entity.passive.EntityVillager;
import cn.nukkit.entity.passive.EntityWolf;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;

import java.io.*;
import org.json.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class UndergroundVillageBuilder extends PluginBase {
	private static final String pluginPath = "plugins/UndergroundVillage/";
	private static final String[] FILE_PATHS = { pluginPath + "layout.json", pluginPath + "layout.csv",
			pluginPath + "layout.xlsx" };

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
					String structureItem = structure.get(y).get(z).get(x);
					Block blockToSet;
					if (structureItem.startsWith("Block.")) {
						blockToSet = getBlockWithAdjustments(player, structureItem, direction);
					} else {
						blockToSet = getBlockWithAdjustments(player, "Block.AIR", direction);
					}
					level.setBlock(new Vector3(baseX + x, baseY + y, baseZ + z), blockToSet, true, true);

					if (structureItem.startsWith("Villager")) {
						spawnVillager(level, new Vector3(baseX + x, baseY + y, baseZ + z), structureItem);
					}
					else if (structureItem.startsWith("Animal")) {
						spawnAnimal(level, new Vector3(baseX + x, baseY + y, baseZ + z), structureItem);
					}
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
		// This method deals with items that require meta, setDamage, or uncovered
		// assets
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
				blockID = 64;
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
				blockID = 5;
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
			if (blockName.trim().equals("")) {
				return Block.AIR;
			} else {
				Field field = Block.class.getField(blockName.toUpperCase());
				return field.getInt(null);
			}

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



	public void spawnVillager(Level level, Vector3 position, String professionName) {
		// Convert profession name to ID using a switch statement
		professionName = professionName.replace("Villager.", "");
		int profession;
		switch (professionName.toLowerCase()) {
			case "farmer":
				profession = 0;
				break;
			case "librarian":
				profession = 1;
				break;
			case "priest":
				profession = 2;
				break;
			case "blacksmith":
				profession = 3;
				break;
			case "butcher":
				profession = 4;
				break;
			case "nitwit":
				profession = 5;
				break;
			default:
				profession = 5; // Default to Nitwit if unknown
		}

		// Create the NBT data for the villager
		CompoundTag nbt = new CompoundTag()
				.putList(new ListTag<DoubleTag>("Pos")
						.add(new DoubleTag("", position.x))
						.add(new DoubleTag("", position.y))
						.add(new DoubleTag("", position.z)))
				.putList(new ListTag<DoubleTag>("Motion")
						.add(new DoubleTag("", 0.0))
						.add(new DoubleTag("", 0.0))
						.add(new DoubleTag("", 0.0)))
				.putList(new ListTag<FloatTag>("Rotation")
						.add(new FloatTag("", 0.0f)) // Default yaw
						.add(new FloatTag("", 0.0f))) // Default pitch
				.putString("CustomName", "Villager")
				.putBoolean("CustomNameVisible", false)
				.putInt("Profession", profession)
				.putBoolean("NoAI", false); // Set to true to prevent movement

		EntityVillager villager = new EntityVillager(level.getChunk((int) position.x >> 4, (int) position.z >> 4), nbt);
		villager.spawnToAll();
	}



	public void spawnAnimal(Level level, Vector3 position, String animalType) {
		animalType = animalType.replace("Animal.", "");

		int chunkX = (int) position.x >> 4;
		int chunkZ = (int) position.z >> 4;

		if (!level.isChunkLoaded(chunkX, chunkZ)) {
			System.out.println("Chunk not loaded, cannot spawn entity at " + position);
			return;
		}

		// Create default NBT
		CompoundTag nbt = new CompoundTag()
			.putList(new ListTag<DoubleTag>("Pos")
				.add(new DoubleTag("", position.x))
				.add(new DoubleTag("", position.y))
				.add(new DoubleTag("", position.z)))
			.putList(new ListTag<DoubleTag>("Motion")
				.add(new DoubleTag("", 0.0))
				.add(new DoubleTag("", 0.0))
				.add(new DoubleTag("", 0.0)))
			.putList(new ListTag<FloatTag>("Rotation")
				.add(new FloatTag("", 0.0f)) // Default yaw
				.add(new FloatTag("", 0.0f))); // Default pitch

		// Declare entity variable
		Entity entity = null;
		Random random = new Random();

		// Switch statement to determine entity type
		switch (animalType.toLowerCase()) {
			case "cow":
				entity = new EntityCow(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "sheep":
				entity = new EntitySheep(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "pig":
				entity = new EntityPig(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "chicken":
				entity = new EntityChicken(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "wolf":
				entity = new EntityWolf(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "fox":
				entity = new EntityFox(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "rabbit":
				entity = new EntityRabbit(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "polar_bear":
				entity = new EntityPolarBear(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "horse":
				int horseVariant = random.nextInt(7); // 0-6 are valid horse colors
				nbt.putInt("Variant", horseVariant);
				entity = new EntityHorse(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "salmon":
				entity = new EntitySalmon(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "cod":
				entity = new EntityCod(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "pufferfish":
				entity = new EntityPufferfish(level.getChunk(chunkX, chunkZ), nbt);
				break;
			case "tropical_fish":
				entity = new EntityTropicalFish(level.getChunk(chunkX, chunkZ), nbt);
				break;
			default:
				System.out.println("Invalid animal type: " + animalType);
				return;
		}

		if (entity != null) {
			level.addEntity(entity);
			System.out.println("Successfully spawned " + animalType + " at " + position);
		}
}

}
