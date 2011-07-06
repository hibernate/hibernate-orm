/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Extends {@link javax.persistence.NamedQuery} with Hibernate features
 *
 * @author Carlos Gonzalez-Cadenas
 */
@Target( { TYPE, PACKAGE })
@Retention(RUNTIME)
public @interface NamedQuery {

	/**
	 * @return returns the name of this {@code NamedQuery}
	 */
	String name();

	/**
	 * @return returns the query string for this {@code NamedQuery}
	 */
	String query();

	/**
	 * @return returns the flush mode for this query
	 */
	FlushModeType flushMode() default FlushModeType.PERSISTENCE_CONTEXT;

	/**
	 * @return returns whether this query is cacheable or not
	 */
	boolean cacheable() default false;

	/**
	 * @return returns the the cache region to use
	 */
	String cacheRegion() default "";

	/**
	 * @return returns the number of rows fetched by the JDBC Driver per database round-trip
	 */
	int fetchSize() default -1;

	/**
	 * @return return the query timeout in seconds
	 */
	int timeout() default -1;

	/**
	 * @return returns the comment added to the SQL query (useful for the DBA)
	 */
	String comment() default "";

	/**
	 * @return returns the cache mode used for this query
	 */
	CacheModeType cacheMode() default CacheModeType.NORMAL;

	/**
	 * @return returns whether the results are fetched in read-only mode or not. Default is {@code false}
	 */
	boolean readOnly() default false;
}
