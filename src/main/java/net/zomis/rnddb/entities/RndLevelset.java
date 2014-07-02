package net.zomis.rnddb.entities;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import net.zomis.chunks.ChunkRead;
import net.zomis.rnddb.files.RocksLevel;
import net.zomis.utils.MD5;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Entity
public class RndLevelset {
	private static final Logger logger = LogManager.getLogger(RndLevelset.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long	id;

	private String	name;
	private String	author;
	
	private String path;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s. (%d--%d, %d levels found)", author, name, firstLevel, levelCount - firstLevel, levels.size());
	}
	
	@OneToMany(mappedBy = "levelset", cascade = { CascadeType.PERSIST })
	private List<RndLevel> levels = new ArrayList<>();

	private int	firstLevel;
	private int	levelCount;

	@ManyToOne(cascade = CascadeType.PERSIST)
	private RndLevelset	parent;

	private boolean	levelGroup;

	private String	checksum;

	@Transient
	private File	confPath;

	public int getFirstLevel() {
		return firstLevel;
	}
	
	public int getLevelCount() {
		return levelCount;
	}
	
	public List<RndLevel> getLevels() {
		return levels;
	}
	
	public void readFiles() {
		if (this.confPath == null)
			throw new IllegalStateException("No path known.");
		
		File directory = confPath.getParentFile();
		int success = 0;
		int total = 0;
		for (File file : directory.listFiles()) {
			if (file.isDirectory())
				continue;
			if (!file.getName().matches("\\d{3}\\.level"))
				continue;
			
			try (DataInputStream dataIn = new DataInputStream(new FileInputStream(file))) {
				RocksLevel result = new ChunkRead().readFile(dataIn, RocksLevel.class);
				if (result != null) {
					success++;
					RndLevel level = new RndLevel();
					level.setFromLevel(result, this, file);
//					level.setFiledata(Files.readAllBytes(file.toPath()));
					this.addLevel(level);
				}
			}
			catch (IOException e) {
				logger.error("Error reading " + file, e);
			}
			total++;
		}
		if (total > 0) {
			logger.info("Directory " + directory + ": " + success + " / " + total);
		}
	}
	
	public void readFromInfo(File conf, RndLevelset parent) {
		try {
			this.confPath = conf;
			List<String> lines = Files.readAllLines(conf.toPath(), StandardCharsets.ISO_8859_1);
			lines.replaceAll(String::trim);
			lines.removeIf(str -> str.startsWith("#"));
			lines.removeIf(str -> !str.contains(":"));
			Function<String, String> key = str -> str.substring(0, str.indexOf(":"));
			Map<String, String> map = lines.stream().collect(Collectors.groupingBy(key, Collectors.reducing("", (a, str) -> str.substring(str.indexOf(":") + 1).trim())));
			this.author = map.get("author");
			this.name = map.get("name");
			this.firstLevel = Integer.parseInt(map.getOrDefault("first_level", "0"));
			this.levelCount = Integer.parseInt(map.getOrDefault("levels", "0"));
			this.levelGroup = Boolean.parseBoolean(map.getOrDefault("level_group", "false"));
			this.parent = parent;
			logger.trace(new TreeMap<>(map));
		}
		catch (Exception e) {
			Logger.getLogger(RndLevelset.class).error("Cannot read " + conf.getAbsolutePath(), e);
		}
	}

	@PrePersist
	public void calcMD5() {
		StringBuilder str = new StringBuilder();
		str.append(this.author);
		str.append(this.name);
		str.append(this.firstLevel);
		str.append(this.levelCount);
		str.append(this.levelGroup);
		levels.stream().filter(level -> level.getMd5() == null).forEach(level -> {
			level.calcMD5();
			str.append(level.getNumber());
			str.append(level.getMd5());
		});
		
		this.checksum = MD5.md5(str.toString());
		
		logger.debug(this);
		levels.forEach(level -> logger.debug(level + ": " + level.getMd5()));
	}
	
	public void addLevel(RndLevel level) {
		this.levels.add(level);
	}
	
	public RndLevelset getParent() {
		return parent;
	}
	
	public boolean isLevelGroup() {
		return levelGroup;
	}
	
	public String getChecksum() {
		return checksum;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
}
