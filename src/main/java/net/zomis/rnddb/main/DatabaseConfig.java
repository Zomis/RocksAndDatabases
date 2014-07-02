package net.zomis.rnddb.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


public class DatabaseConfig {

	public static EntityManagerFactory fromFile(File file) throws IOException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(file));
		return Persistence.createEntityManagerFactory("rnddb", properties);
	}
	
	public static EntityManagerFactory localhostEmbedded() {
		Map<String, String> config = new HashMap<>();
		config.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver");
		config.put("javax.persistence.jdbc.url", "jdbc:hsqldb:file:demodb");
		config.put("javax.persistence.jdbc.user", "sa");
		config.put("javax.persistence.jdbc.password", "");
		config.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		return Persistence.createEntityManagerFactory("rnddb", config);
	}
	
	
}
