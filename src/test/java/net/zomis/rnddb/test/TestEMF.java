package net.zomis.rnddb.test;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TestEMF {
	public static EntityManagerFactory localhostTest() {
		Map<String, String> config = new HashMap<>();
		config.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver");
		config.put("javax.persistence.jdbc.url", "jdbc:hsqldb:file:tempdb");
		config.put("javax.persistence.jdbc.user", "sa");
		config.put("javax.persistence.jdbc.password", "");
		config.put("hibernate.hbm2ddl.auto", "create-drop");
		config.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		return Persistence.createEntityManagerFactory("rnddb", config);
	}

}
