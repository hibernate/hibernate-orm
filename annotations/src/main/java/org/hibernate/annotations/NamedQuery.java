//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Extends {@link javax.persistence.NamedQuery} with Hibernate features
 *
 * @author Carlos González-Cadenas
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedQuery {

	/** the name of the NamedQuery */
	String name();
	/** the Query String for the NamedQuery */
	String query();
	/** the flush mode for the query */
	FlushModeType flushMode() default FlushModeType.PERSISTENCE_CONTEXT;
	/** mark the query as cacheable or not */
	boolean cacheable() default false;
	/** the cache region to use */
	String cacheRegion() default "";
	/** the number of rows fetched by the JDBC Driver per roundtrip */
	int fetchSize() default -1;
	/**the query timeout in seconds*/
	int timeout() default -1;
	/**comment added to the SQL query, useful for the DBA */
	String comment() default "";
	/**the cache mode used for this query*/
	CacheModeType cacheMode() default CacheModeType.NORMAL;
	/**marks whether the results are fetched in read-only mode or not*/
	boolean readOnly() default false;

}
