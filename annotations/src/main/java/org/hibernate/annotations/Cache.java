//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Add caching strategy to a root entity or a collection
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cache {
	/** concurrency strategy chosen */
	CacheConcurrencyStrategy usage();
	/** cache region name */
	String region() default "";
	/**
	 * whether or not lazy-properties are included in the second level cache
	 * default all, other value: non-lazy
	 */
	String include() default "all";
}
