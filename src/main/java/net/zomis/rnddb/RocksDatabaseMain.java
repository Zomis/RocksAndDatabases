package net.zomis.rnddb;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

import net.zomis.rnddb.entities.RndLevelset;


public class RocksDatabaseMain {

	public static void main(String[] args) {
		/* Prio 1: Scan levels, save to local database (Postgres)
		 * 
		 * - levelsets to .zip ---> Save as file, not bytearraay in database
		 * - levelsets is the interesting things, not levels and/or levelgroups
		 * - use md5-sum of levelset/zip to determine if it already exists locally/remotely
		 * - 
		 * 
		 * Client: LOAD
		 * Server: Fetches iformation about all levelsets, sends to client as JSON
		 * 
		 * Client: DLOD md5
		 * Server: LSET info
		 * Server: FILE `RndFile` JSON data
		 * Server: DATA hex
		 * Server: DATA hex
		 * Server: DATA hex
		 * Server: DATA hex
		 * Server: FILE `RndFile` JSON data
		 * Server: DATA hex
		 * Server: DATA hex
		 * Server: DEND
		 * 
		 * 
		 * ? levelgroup table
		 * 
		 * 
		 * Prio 2: Request levels/levelsets from a server
		 * String location: server:port/id
		 * String location: path
		 * 
		 * Prio 3: Upload levels/levelsets to server
		 * - when uploading, check if MD5 (of levelset_name+level_number + file contents) already exists or not. If it does, don't add another one.
		 * 
		 * Prio 4: Modifying/Branching existing levels
		 * - requires users and permissions?
		 * - no user restrictions in uploading/downloading
		 * Big potential problem: updating existing levels.
		 * 
		 * Prio 5: Tags? (on levels and levelsets)
		 * 
		 * USER
		 * 
		 * 
		 * LEVELSET
		 * 
		 * 
		 * LEVEL
		 * 
		 * 
		 * TAPE
		 * 
		 * TAGS
		 * 
		 * 
YES - ability to add levels / levelsets / larger collections to my local set
YES - hierarchical organization and controls making it adequately easy for me to collect all levels known to the repository
YES - including a way to confirm / crosscheck that I really have it all
YES - easy way to upload my own created levels, creating my own personal levelset or collection of levelsets which other users can access
XXXXXXXX - if I change or add a level to my local personal levelset(s), easy way to sync my changes up to the repository
XXXXXXXX - if I change a level created by someone else, a way to upload it as a "proposed change" such that the repository will inform the original author (or designated maintainer), and also make it available to other users, but not as the default downloaded version of that level

LATER - when I successfully play a level, be able to upload my solution
PERHAPS(check with Holger) - (I would hope that the upload receiver would have a way to invoke the game engine and verify that my tape is actually a valid solution of the level I'm tagging it to -- I realize there are potentially some fairly difficult issues to solve for this to work well...)
HOLGER - be able to configure RND to automatically upload such solutions whenever I finish a level (again, this has difficulties -- but "OMG that'd be a lot of space" actually probably isn't one, since it would only happen for users who went to the trouble of registering with the repository and turning on the option, and tapes are quite small anyway)
LATER - be able to browse other people's solutions to a level
LATER - hopefully the site would be able to evaluate solutions to determine which was the "best", though it's not immediately clear to me what "best" might really mean. Shortest amount of time? Shortest path of motion? Highest "score"? But the scoring in RND levels seems almost completely superfluous to me...
LATER - if there's a "best" function then of course the easiest / default way of getting a solution for the current level would grab the one ID'd as "best"
LATER - some sort of "leader board" showing, for each registered player, how many levels they've solved; how many "best" solutions they were responsible for; average "goodness" if the evaluation function can actually produce a sensible number for this (e.g. "best solution time / user's solution time" -- if I took twice as long, mine's only worth 0.5 "goodness" point)
LATER - obviously the game shouldn't offer access to an online solution until you've solved it locally; equally obviously, that'd be very easy to cheat, so the "prevention" shouldn't be any stronger than the current prevention of skipping levels. That is, (1) an option you need to turn on to say "yes, let me request tapes for levels I haven't personally solved" plus (2) an interactive "are you sure you want to see a spoiler for this level?" question
LATER - user ratings of collections, levelsets and levels
		 * 
		 * index-fil i/för varje katalog för att lista alla filer och deras md5-summor
		 * 
		 * 
		 * 
		 * */
		
		
		System.out.println("Creating EMF");
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("rnddb");
		
		System.out.println("Creating EM");
		EntityManager em = emf.createEntityManager();
		
		RndLevelset entity = new RndLevelset();
		entity.setName("Random " + Math.random());
		System.out.println("Persisting " + entity);
		em.getTransaction().begin();
		em.persist(entity);
		em.getTransaction().commit();
		
		CriteriaQuery<RndLevelset> q = emf.getCriteriaBuilder().createQuery(RndLevelset.class);
		q.from(RndLevelset.class);
		
		TypedQuery<RndLevelset> query = em.createQuery(q); // sadf.createQuery("SELECT lset FROM RndLevelset lset");
		
		List<RndLevelset> sets = query.getResultList();
		System.out.println(sets);
		
//		System.getenv().forEach((key, value) -> System.out.println(key + ": " + value));
		
		em.close();
		emf.close();
		
	}

}
