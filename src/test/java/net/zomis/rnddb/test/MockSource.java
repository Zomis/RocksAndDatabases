package net.zomis.rnddb.test;

import java.util.List;

import net.zomis.rnddb.RndScanner;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.rnddb.host.RndDbSource;

public class MockSource implements RndDbSource {

	private final List<RndLevelset>	all = RndScanner.scanLevels(TestFiles.fileFor("userdir"), lset -> {});

	public MockSource() {
	}
	
	@Override
	public void saveLevelSet(RndLevelset value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RndLevelset getLevelSet(String md5) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<RndLevelset> getAllLevelSets() {
		return all;
	}

	@Override
	public RndLevel getLevel(String md5) {
		return null;
	}

}
