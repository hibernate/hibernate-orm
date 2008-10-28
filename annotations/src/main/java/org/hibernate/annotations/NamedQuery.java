/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Extends {@link javax.persistence.NamedQuery} with Hibernate features
 *
 * @author Carlos Gonz�lez-Cadenas
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
