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
 * Extends {@link javax.persistence.NamedNativeQuery} with Hibernate features.
 *
 * @author Emmanuel Bernard
 *
 * @see org.hibernate.SQLQuery
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedNativeQuery {
	/**
	 * The name.  It is a named query after all :)
	 */
	String name();

	/**
	 * The SQL query string.
	 */
	String query();

	/**
	 * The result Class.  Should not be used in conjunction with {@link #resultSetMapping()}
	 */
	Class resultClass() default void.class;

	/**
	 * The name of a SQLResultSetMapping to use.  Should not be used in conjunction with {@link #resultClass()}.
	 */
	String resultSetMapping() default "";

	/**
	 * The flush mode for the query.
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
	 * Does the SQL ({@link #query()}) represent a call to a procedure/function?
	 */
	boolean callable() default false;

	/**
	 * A comment added to the SQL query.  Useful when engaging with DBA.
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
