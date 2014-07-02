package net.zomis.chunks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ChunkEntry {
	int element() default -1;
	int order();
	int size() default -1;
	SaveType save() default SaveType.WHEN_CHANGED;
	int standard() default 0;
	
}
