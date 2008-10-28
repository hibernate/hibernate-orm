//$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Array of generic generator definitions
 *
 * @author Paul Cowan
 */
@Target({PACKAGE, TYPE})
@Retention(RUNTIME)
public @interface GenericGenerators {
	GenericGenerator[] value();
}

