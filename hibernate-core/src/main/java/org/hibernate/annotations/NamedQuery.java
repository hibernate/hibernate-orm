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
 * Extends {@link javax.persistence.NamedQuery} with Hibernate features.
 *
 * @author Carlos Gonzalez-Cadenas
 *
 * @see org.hibernate.Query
 */
@Target( { TYPE, PACKAGE })
@Retention(RUNTIME)
public @interface NamedQuery {

	/**
	 * The name of this {@code NamedQuery}.
	 */
	String name();

	/**
	 * The query string for this {@code NamedQuery}.
	 */
	String query();

	/**
	 * The flush mode for this query.
	 */
	FlushModeType flushMode() default FlushModeType.PERSISTENCE_CONTEXT;

	/**
	 * Whether the query (results) is cacheable or not.  Default is {@code false}, that is not cacheable.
	 */
	boolean cacheable() default false;

	/**
	 * If the query results are cacheable, name the query cache region to use.
	 */
	String cacheRegion() default "";

	/**
	 * The number of rows fetched by the JDBC Driver per trip.
	 */
	int fetchSize() default -1;

	/**
	 * The query timeout (in seconds).  Default is no timeout.
	 */
	int timeout() default -1;

	/**
	 * A comment added to the generated SQL query.  Useful when engaging with DBA.
	 */
	String comment() default "";

	/**
	 * The cache mode used for this query.  This refers to entities/collections returned from the query.
	 */
	CacheModeType cacheMode() default CacheModeType.NORMAL;

	/**
	 * Whether the results should be read-only.  Default is {@code false}.
	 */
	boolean readOnly() default false;
}
