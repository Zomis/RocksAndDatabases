package net.zomis.rnddb.files;

import java.io.IOException;

import net.zomis.chunks.ChunkEntry;
import net.zomis.chunks.ChunkException;
import net.zomis.chunks.MicroChunk;
import net.zomis.chunks.OnUnknownChunk;
import net.zomis.chunks.SerializationContext;
import net.zomis.utils.ZSubstr;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RocksLevel {

	private static final Logger logger = LogManager.getLogger(RocksLevel.class);
	private static final int MAX_LEVEL_NAME_LEN	= 32;
	private static final int LEVEL_SCORE_ELEMENTS	= 16;
	private static final int STD_ELEMENT_CONTENTS	= 4;
	private static final int LEVEL_CHUNK_HEAD_UNUSED	= 0;

	@MicroChunk(value = "RND1")
	@ChunkEntry(size = 4, order = 1)
	private Void empty; 
	
	@ChunkEntry(order = 2)
	private CaveData cave;
	
	@ChunkEntry(order = 3)
	private VersData vers = new VersData();
	
	@ChunkEntry(order = 4)
	private DateData date;
	
	@ChunkEntry(order = 5)
	@MicroChunk(value = "NAME", isMicro = false)
	private String name;
	
	@ChunkEntry(order = 6)
	@MicroChunk(value = "AUTH", isMicro = false)
	private String author = "anonymous";
	
	@ChunkEntry(order = 7)
	private InfoData info = new InfoData();
	
	@ChunkEntry(order = 8)
	private BodyData body = new BodyData();

	public InfoData getInfo() {
		return info;
	}
	
	@ChunkEntry(order = 9)
	private ElemData elem;
	
	@ChunkEntry(order = 10)
	private EnvelopeData envelope;
	
	@ChunkEntry(order = 11)
	private CustomElementXData customElement;
	
	private boolean	encoding_16bit_field = true;
	
	// TODO: element-specific values for things such as ELEM-chunks. See LoadLevel_MicroChunk and LevelFileConfigInfo in files.c.
	// Also see chunk_config_ELEM, chunk_config_NOTE, chunk_config_CUSX_base...
	// TODO: Support multiple chunks of CUSX, ELEM...
	
	
	@OnUnknownChunk
	public void unknownChunk(String name, SerializationContext context) throws IOException {
		if (!name.matches("^[A-Z\\d]+$")) {
			throw new IllegalArgumentException("Unexpected chunk name: " + name);
		}
		
		switch (name) {
			case "HEAD":
//				System.out.println("Reading " + name + context.getRead());
				int chunkSize = context.readInt();
				LoadLevel_HEAD(context, chunkSize);
				break;
			case "ROCK":
				// figure out file format
				String expectedIdentifier = "ROCKSNDIAMONDS_LEVEL_FILE_VERSION_x.x";
				String readIdentifier = context.readString(expectedIdentifier.length() - name.length());
				if (!readIdentifier.matches("SNDIAMONDS_LEVEL_FILE_VERSION_\\d\\.\\d")) {
					throw new ChunkException("Unrecognized file format: " + readIdentifier);
				}
				int majorVers = Integer.parseInt(ZSubstr.substr(readIdentifier, -3, 1));
				int minorVers = Integer.parseInt(ZSubstr.substr(readIdentifier, -1, 1));
				this.vers.setFileVersion(VERSION_IDENT(majorVers, minorVers, 0, 0));
				context.skip(1);
				if (majorVers == 1 && minorVers == 0) {
					// Very old style!
					LoadLevel_HEAD(context, 0x50);
					body.read(context, this);
				}
				break;
			case "CONT":
				int storedChunkSize = context.readInt();
				int headerSize = 4;
				int contentSize = 8 * 3 * 3;
				int chunkSizeExpected = headerSize + contentSize;
				
				logger.debug("Reading CONT: " + chunkSizeExpected + " stored size is " + storedChunkSize);
//				System.out.println("Version: " + vers.getFileVersion() + " 16 bit? " + encoding_16bit_field);
				
				if (this.encoding_16bit_field && vers.getFileVersion() < VERSION_IDENT(2, 0, 0, 0)) {
//					System.out.println("Modifying chunk size expected because of old RnD Bug.");
					chunkSizeExpected += contentSize;
				}
//				System.out.println("Skipping CONT: " + chunkSizeExpected);
				context.skip(chunkSizeExpected);
				break;
			default:
				logger.trace("Unknown Chunk Name: " + name + ", skipping it. At " + context.getRead());
				int i = context.readInt();
				context.skip(i);
				
		}
	}
	
	static int VERSION_IDENT(int a, int b, int c, int d) {
		return (a) * 1000000 + (b) * 10000 + (c) * 100 + (d);
	}
	
/*
  if (level->file_version < FILE_VERSION_1_2)
  {
    // level files from versions before 1.2.0 without chunk structure 
    LoadLevel_HEAD(file, LEVEL_CHUNK_HEAD_SIZE,         level);
    LoadLevel_BODY(file, level->fieldx * level->fieldy, level);
  }
*/
	
	public String getName() {
		return name;
	}
	
	public String getAuthor() {
		return author;
	}

	
	
	private void LoadLevel_HEAD(SerializationContext context, int chunkSize) throws IOException {
//	  int initial_player_stepsize;
//	  int initial_player_gravity;
//	  int i, x, y;

	  if (chunkSize != 0x50) {
		  throw new IllegalStateException(chunkSize + " was not expected.");
	  }
		
	  this.info.setWidth(context.readByte());
	  this.info.setHeight(context.readByte());
	  logger.debug("Size: " + info.getWidth() + " * " + info.getHeight());

	  this.info.setTime(context.readWord());
	  this.info.setGemsNeeded(context.readWord());

	  this.name = context.readString(MAX_LEVEL_NAME_LEN);
	  logger.debug("Name read: " + name);

//	  for (i = 0; i < LEVEL_SCORE_ELEMENTS; i++)
//	    level->score[i] = getFile8Bit(file);
	  context.skip(LEVEL_SCORE_ELEMENTS);

//	  level->num_yamyam_contents = STD_ELEMENT_CONTENTS;
//	  for (i = 0; i < STD_ELEMENT_CONTENTS; i++)
//	    for (y = 0; y < 3; y++)
//	      for (x = 0; x < 3; x++)
//		level->yamyam_content[i].e[x][y] = getMappedElement(getFile8Bit(file));
	  context.skip(STD_ELEMENT_CONTENTS * 3 * 3);

	  context.skip(4);
//	  level->amoeba_speed		= getFile8Bit(file);
//	  level->time_magic_wall	= getFile8Bit(file);
//	  level->time_wheel		= getFile8Bit(file);
//	  level->amoeba_content		= getMappedElement(getFile8Bit(file));

	  context.skip(1);
//	  initial_player_stepsize	= (getFile8Bit(file) == 1 ? STEPSIZE_FAST :
//					   STEPSIZE_NORMAL);

//	  for (i = 0; i < MAX_PLAYERS; i++)
//	    level->initial_player_stepsize[i] = initial_player_stepsize;

	  context.skip(1);
//	  initial_player_gravity	= (getFile8Bit(file) == 1 ? TRUE : FALSE);

//	  for (i = 0; i < MAX_PLAYERS; i++)
//	    level->initial_player_gravity[i] = initial_player_gravity;

	  this.encoding_16bit_field	= context.readBoolean();
//	  level->em_slippery_gems	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
	  context.skip(1);

	  context.skip(3);
//	  level->use_custom_template	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//	  level->block_last_field	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//	  level->sp_block_last_field	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
	  
	  context.skip(5);
//	  level->can_move_into_acid_bits = getFile32BitBE(file);
//	  level->dont_collide_with_bits = getFile8Bit(file);

	  context.skip(6);
//	  level->use_spring_bug		= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//	  level->use_step_counter	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//
//	  level->instant_relocation	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//	  level->can_pass_to_walkable	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//	  level->grow_into_diggable	= (getFile8Bit(file) == 1 ? TRUE : FALSE);
//
//	  level->game_engine_type	= getFile8Bit(file);

	  	context.skip(LEVEL_CHUNK_HEAD_UNUSED);
	}  
	
	
	public boolean isEncoding_16bit_field() {
		return encoding_16bit_field;
	}
	
	public VersData getVers() {
		return vers;
	}

	public BodyData getBody() {
		return body;
	}
	
}
