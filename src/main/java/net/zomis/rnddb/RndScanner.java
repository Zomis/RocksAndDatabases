package net.zomis.rnddb;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

import net.zomis.chunks.ChunkRead;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;
import net.zomis.rnddb.files.RocksLevel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RndScanner {
	private Preferences prefs = Preferences.userNodeForPackage(RndScanner.class);
	private static final Logger logger = LogManager.getLogger(RndScanner.class);
	
	public static final String GAME_DIR = "rnd";
	public static final String USER_DIR = "user";
	
	public File inputDirectory(String key) {
		String str = prefs.get(key, null);
		final File file;
		if (str == null) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				prefs.put(key, file.getAbsolutePath());
			}
			else return null; 
		}
		else file = new File(str);
		
		if (file.exists() && file.isDirectory())
			return file;
		else {
			prefs.remove(key);
			return inputDirectory(key);
		}
		
	}
	
	public static List<RndLevelset> scan(File directory, Consumer<RndLevelset> scanCallback) {
		return scanDirectory(directory, null, scanCallback);
	}
	
	private static List<RndLevelset> scanDirectory(File directory, RndLevelset parent, Consumer<RndLevelset> scanCallback) {
		List<RndLevelset> list = new ArrayList<>();
		int success = 0;
		int total = 0;
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				RndLevelset levelSet = new RndLevelset();
				File conf = new File(file, "levelinfo.conf");
				if (!conf.exists()) {
					logger.debug("Is directory but no levelinfo found: " + file);
					continue;
				}
				levelSet.readFromInfo(conf, parent);
				list.add(levelSet);
				scanCallback.accept(levelSet);
				list.addAll(scanDirectory(file, levelSet, scanCallback));
				continue;
			}
			
			if (!file.getName().endsWith(".level")) {
				continue;
			}
		}
		
		return list;
	}
	
	public static File getLevelsDir(File base) {
		return new File(base, "levels");
	}
	
	
}
