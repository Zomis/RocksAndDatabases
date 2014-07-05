package net.zomis.rnddb.host;

import java.util.List;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;

public interface RndDbSource {

	RndLevelset saveLevelSet(RndLevelset value);

	RndLevelset getLevelSet(String md5);
	
	@Deprecated
	RndLevel getLevel(String md5);

	List<RndLevelset> getAllLevelSets();
	
	default void close() {}

	List<RndFile> getFilesInSet(Long id);
	
	
}
