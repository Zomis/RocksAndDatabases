package net.zomis.rnddb.entities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import net.zomis.rnddb.files.RocksLevel;
import net.zomis.utils.MD5;
import net.zomis.utils.ZSubstr;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class RndLevel {
	private static final Logger logger = LogManager.getLogger(RndLevel.class);
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long	id;

	@ManyToOne
	@JsonIgnore
	private RndLevelset levelset;
	
	@Transient
	@JsonIgnore
	private File file;
	
	private String	title;
	private String	author;

	private Integer	number;
	private String	md5;

	private byte[]	filedata;

	private Integer	fieldx;
	private Integer	fieldy;

	private Integer	timeLimit;
	private Integer	gemsNeeded;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public String getMd5() {
		return md5;
	}
	
	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public byte[] getFiledata() {
		return filedata;
	}

	public void setFiledata(byte[] filedata) {
		this.filedata = filedata;
	}

	public Integer getFieldx() {
		return fieldx;
	}

	public void setFieldx(Integer fieldx) {
		this.fieldx = fieldx;
	}

	public Integer getFieldy() {
		return fieldy;
	}

	public void setFieldy(Integer fieldy) {
		this.fieldy = fieldy;
	}

	public Integer getTimeLimit() {
		return timeLimit;
	}
	
	public void setTimeLimit(Integer timeLimit) {
		this.timeLimit = timeLimit;
	}
	
	public Integer getGemsNeeded() {
		return gemsNeeded;
	}

	public void setGemsNeeded(Integer gemsNeeded) {
		this.gemsNeeded = gemsNeeded;
	}

	@PrePersist
	public void calcMD5() {
		if (this.file == null) {
			logger.warn("Unable to calculate MD5 on null file: " + this.author + " - " + this.title + " file " + number + " in " + levelset);
			return;
		}
		try {
			this.filedata = Files.readAllBytes(this.file.toPath());
			this.md5 = MD5.md5(this.filedata);
			logger.info(new ObjectMapper().writeValueAsString(this));
		}
		catch (IOException e) {
			logger.error("Cannot calc MD5 of " + this, e);
		}
	}
	
	public void setFromLevel(RocksLevel level, RndLevelset levelset, File file) {
		this.levelset = levelset;
		this.author = level.getAuthor();
		this.fieldx = level.getInfo().getWidth();
		this.fieldy = level.getInfo().getHeight();
		this.gemsNeeded = level.getInfo().getGemsNeeded();
		this.timeLimit = level.getInfo().getTime();
		this.title = level.getName();
		try {
			this.number = Integer.parseInt(ZSubstr.substr(file.getName(), 0, 3));
		}
		catch (NumberFormatException ex) {
		}
		this.file = file;
	}
	
	public String serialize() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		}
		catch (JsonProcessingException e) {
			logger.error("", e);
			return null;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%3s %s: %s", this.number, this.author, this.title);
	}

	public void clearBigData() {
		this.filedata = null;
	}
}
