package net.zomis.chunks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface MicroChunk {
	boolean isMicro() default true; // support non-micros as well (such as old RND chunks)
	int size() default -1;
	String value();
}
