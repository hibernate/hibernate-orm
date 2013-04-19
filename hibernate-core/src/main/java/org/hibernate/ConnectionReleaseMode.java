/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

/**
 * Defines the various policies by which Hibernate might release its underlying
 * JDBC connection.
 *
 * @author Steve Ebersole
 */
public enum ConnectionReleaseMode{
	/**
	 * Indicates that JDBC connection should be aggressively released after each 
	 * SQL statement is executed. In this mode, the application <em>must</em>
	 * explicitly close all iterators and scrollable results. This mode may
	 * only be used with a JTA datasource.
	 */
	AFTER_STATEMENT,

	/**
	 * Indicates that JDBC connections should be released after each transaction 
	 * ends (works with both JTA-registered synch and HibernateTransaction API).
	 * This mode may not be used with an application server JTA datasource.
	 * <p/>
	 * This is the default mode starting in 3.1; was previously {@link #ON_CLOSE}.
	 */
	AFTER_TRANSACTION,

	/**
	 * Indicates that connections should only be released when the Session is explicitly closed 
	 * or disconnected; this is the legacy (Hibernate2 and pre-3.1) behavior.
	 */
	ON_CLOSE;

	/**
	 * Alias for {@link ConnectionReleaseMode#valueOf(String)} using upper-case version of the incoming name.
	 *
	 * @param name The name to parse
	 *
	 * @return The matched enum value.
	 */
	public static ConnectionReleaseMode parse(final String name) {
		return ConnectionReleaseMode.valueOf( name.toUpperCase() );
	}
}
