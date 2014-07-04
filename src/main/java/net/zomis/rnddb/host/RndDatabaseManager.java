package net.zomis.rnddb.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import net.zomis.rnddb.entities.RndFile;
import net.zomis.rnddb.entities.RndLevel;
import net.zomis.rnddb.entities.RndLevelset;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RndDatabaseManager implements AutoCloseable, RndDbSource {

	private final EntityManagerFactory	emf;
	private static final Logger logger = LogManager.getLogger(RndDatabaseManager.class);

	public RndDatabaseManager(EntityManagerFactory emf) {
		this.emf = Objects.requireNonNull(emf);
	}

	@Override
	public void close() {
		emf.close();
	}

	@Override
	public RndLevelset saveLevelSet(RndLevelset value) {
		RndLevelset previous = getLevelSet(value.getChecksum());
		if (value.getParent() != null) {
			logger.info("Parent is: " + value.getParent());
			RndLevelset parent = getLevelSet(value.getParent().getChecksum());
			value.setParent(parent);
		}
		logger.info("Previous: " + previous);
		if (previous != null) {
			logger.error("Duplicate Levelset: " + value + " with sum " + value.getChecksum());
			return null;
		}
		return tryCatch(em -> {
			em.getTransaction().begin();
			logger.info("Persisting " + value);
			em.persist(value);
			em.getTransaction().commit();
			return value;
		});
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<RndLevelset> getAllLevelSets() {
		return tryCatch(em -> {
			Query query = em.createQuery("SELECT lset FROM RndLevelset lset");
			
			List<RndLevelset> levelsets = query.getResultList();
			levelsets.forEach(lset -> lset.getLevels().size()); // fetch lazily
			return new ArrayList<>(levelsets);
		});
	}
	
	@Override
	public List<RndFile> getFilesInSet(Long id) {
		EntityManager em = emf.createEntityManager();
		RndLevelset levelset = em.find(RndLevelset.class, id);
		List<RndFile> files = levelset.getLevels();
		files.size();
		em.close();
		return files;
	}
	
	@Override
	public RndLevelset getLevelSet(String md5) {
		EntityManager em = emf.createEntityManager();
		Query query = em.createQuery("SELECT lset FROM RndLevelset lset WHERE checksum = :md5");
		query.setParameter("md5", md5);
		
		@SuppressWarnings("unchecked")
		List<RndLevelset> levelsets = query.getResultList();
		RndLevelset result = levelsets.stream().findFirst().orElse(null);
		
		if (result != null) {
			result.clearLevelsForSending();
//			result.getLevels().forEach(level -> level.clearBigData()); // don't transport too much data!
		}
		em.close();
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RndLevel getLevel(String md5) {
		return tryCatch(em -> {
			Query query = em.createQuery("SELECT level FROM RndLevel level WHERE checksum = :md5");
			query.setParameter("md5", md5);
			
			List<RndLevel> levels = query.getResultList();
			return new ArrayList<>(levels).stream().findFirst().orElse(null);
		});
	}
	
	private <T> T tryCatch(Function<EntityManager, T> func) {
		EntityManager em = emf.createEntityManager();
		try {
			return func.apply(em);
		}
		catch (Exception ex) {
			logger.error("", ex);
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			return null;
		}
		finally {
			em.close();
		}
	}
	
}
