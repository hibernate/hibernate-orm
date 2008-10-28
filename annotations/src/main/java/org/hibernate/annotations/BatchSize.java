//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Batch size for SQL loading
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface BatchSize {
	/** Strictly positive integer */
	int size();
}
