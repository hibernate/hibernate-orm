//$Id$
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * A parameter definition
 *
 * @author Emmanuel Bernard
 */
@Target({})
@Retention(RUNTIME)
public @interface ParamDef {
	String name();

	String type();
}
