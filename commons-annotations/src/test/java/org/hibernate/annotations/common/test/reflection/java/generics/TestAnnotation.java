package org.hibernate.annotations.common.test.reflection.java.generics;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface TestAnnotation {
	String name() default "abc";
}
