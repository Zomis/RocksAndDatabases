package net.zomis.rnddb.entities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import net.zomis.utils.MD5;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(indexes = {@Index(columnList="md5")})
public class RndFile {

	private static final Logger logger = LogManager.getLogger(RndFile.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long	id;
	
	private String md5;
	private String filename;
	private long size;
	
	@ManyToOne
	private RndLevelset levelset;
	
	public String getMd5() {
		return md5;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void calcMd5() {
		File file = getFile();
		try {
			byte[] filedata = Files.readAllBytes(file.toPath());
			this.md5 = MD5.md5(filedata);
//			logger.info(new ObjectMapper().writeValueAsString(this));
		}
		catch (IOException e) {
			logger.error("Cannot calc MD5 of " + this, e);
		}
		logger.info("MD5 for " + filename);
	}
	
	@JsonIgnore
	public File getFile() {
		return new File(levelset.getAbsolutePath(), this.getFilename());
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public long getSize() {
		return size;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setLevelset(RndLevelset levelset) {
		this.levelset = levelset;
	}

	public void setData(RndLevelset rndLevelset, File root, File file) {
		String filePath = file.getAbsolutePath(); // "/var/data/stuff/xyz.dat";
		String base = root.getAbsolutePath(); // "/var/data";
		String relative = new File(base).toURI().relativize(new File(filePath).toURI()).getPath();
		
		this.filename = relative;
		this.levelset = rndLevelset;
		this.size = file.length();
	}
	
}
