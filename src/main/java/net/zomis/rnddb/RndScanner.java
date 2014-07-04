package net.zomis.rnddb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

import net.zomis.rnddb.entities.RndLevelset;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RndScanner {
	@Deprecated
	private Preferences prefs = Preferences.userNodeForPackage(RndScanner.class);
	
	private static final Logger logger = LogManager.getLogger(RndScanner.class);
	
	@Deprecated
	public static final String GAME_DIR = "rnd";
	
	@Deprecated
	public static final String USER_DIR = "user";
	
	@Deprecated
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
	
	public static List<RndLevelset> scanLevels(File directory, Consumer<RndLevelset> scanCallback) {
		return scanDirectory(directory, new File(directory, "levels"), null, scanCallback);
	}
	
	private static List<RndLevelset> scanDirectory(File rootPath, File directory, RndLevelset parent, Consumer<RndLevelset> scanCallback) {
		if (directory == null || !directory.exists()) {
			throw new IllegalArgumentException("Invalid directory: " + directory);
		}
		List<RndLevelset> list = new ArrayList<>();
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				RndLevelset levelSet = new RndLevelset();
				File conf = new File(file, "levelinfo.conf");
				if (!conf.exists()) {
					logger.debug("Is directory but no levelinfo found: " + file);
					continue;
				}
				levelSet.readFromInfo(rootPath, conf.getParentFile(), parent);
				list.add(levelSet);
				scanCallback.accept(levelSet);
				list.addAll(scanDirectory(rootPath, file, levelSet, scanCallback));
				continue;
			}
		}
		
		return list;
	}
	
	public static File getLevelsDir(File base) {
		return new File(base, "levels");
	}
	
	
}
