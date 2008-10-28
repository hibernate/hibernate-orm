//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Type definition
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface TypeDef {
	String name();

	Class typeClass();

	Parameter[] parameters() default {};
}
