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

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class RndLevelset {
	private static final Logger logger = LogManager.getLogger(RndLevelset.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long	id;

	private String	name;
	private String	author;
	
	@ManyToOne
	private RndLevelset branchFrom;
	
	private String path;

	@OneToMany(cascade = CascadeType.PERSIST, mappedBy = "levelset")
	private List<RndFile> files = new ArrayList<>();
	
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
		return String.format("%s: %s. (%d--%d, %d files)", author, name, firstLevel, levelCount - firstLevel, files.size());
	}
	
	private int	firstLevel;
	private int	levelCount;

	@ManyToOne(cascade = CascadeType.PERSIST)
	private RndLevelset	parent;

	private boolean	levelGroup;

	private String	checksum;

	@Transient
	private File	directory;

	public int getFirstLevel() {
		return firstLevel;
	}
	
	public int getLevelCount() {
		return levelCount;
	}
	
	public List<RndFile> getLevels() {
		return files;
	}
	
	private void scanFiles(File root, File path) {
		int success = 0;
		int total = 0;
		for (File file : path.listFiles()) {
			if (file.isDirectory()) {
				scanFiles(root, file);
				continue;
			}
//			if (!file.getName().matches("\\d{3}\\.level"))
//				continue;
			
			RndFile rndFile = new RndFile();
			rndFile.setData(this, root, file);
			addFile(rndFile);
			
			total++;
		}
		if (total > 0) {
			logger.info("Directory " + path + ": " + success + " / " + total);
		}
	}

	@Deprecated
	List<RndLevel> loadLevels() {
		List<RndLevel> levels = new ArrayList<>();
		for (RndFile rndFile : this.files) {
			File file = new File(this.directory, rndFile.getFilename());
			try (DataInputStream dataIn = new DataInputStream(new FileInputStream(file))) {
				RocksLevel result = new ChunkRead().readFile(dataIn, RocksLevel.class);
		
				if (result != null) {
					RndLevel level = new RndLevel();
					level.setFromLevel(result, this, file);
//					level.setFiledata(Files.readAllBytes(file.toPath()));
					levels.add(level);
				}
			}
			catch (IOException e) {
				logger.error("Error reading " + file, e);
			}
		}
		return levels;
	}
	
	
	public void readFiles() {
		if (this.directory == null)
			throw new IllegalStateException("No path known.");
		
		this.scanFiles(directory, directory);
	}
	
	public void readFromInfo(File rootPath, File directory, RndLevelset parentSet) {
		try {
			this.directory = directory;
			
			String filePath = directory.getAbsolutePath(); // "/var/data/stuff/xyz.dat";
			String base = rootPath.getAbsolutePath(); // "/var/data";
			String relative = new File(base).toURI().relativize(new File(filePath).toURI()).getPath();
			
			this.path = relative;
			File configFile = new File(directory, "levelinfo.conf");
			
			List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.ISO_8859_1);
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
			this.parent = parentSet;
			logger.trace(new TreeMap<>(map));
		}
		catch (Exception e) {
			Logger.getLogger(RndLevelset.class).error("Cannot read " + directory.getAbsolutePath(), e);
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
		files.stream().filter(level -> level.getMd5() == null).forEach(level -> {
			level.calcMd5();
			str.append(level.getFilename());
			str.append(level.getMd5());
		});
		
		this.checksum = MD5.md5(str.toString());
		
		logger.debug(this);
		files.forEach(level -> logger.debug(level + ": " + level.getMd5()));
	}
	
	public void addFile(RndFile level) {
		this.files.add(level);
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

	@JsonIgnore
	public String getAbsolutePath() {
		return this.directory.getAbsolutePath();
	}
}
