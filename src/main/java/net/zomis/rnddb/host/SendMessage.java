package net.zomis.rnddb.host;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class SendMessage {

	private List<RndLevelset> sets;
	private List<RndLevel> levels;
	private String	message;
	
	
	public void setLevels(List<RndLevel> levels) {
		this.levels = levels;
	}
	
	public void setSets(List<RndLevelset> sets) {
		this.sets = sets;
	}
	
	public List<RndLevel> getLevels() {
		return levels;
	}
	
	public List<RndLevelset> getSets() {
		return sets;
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
}
