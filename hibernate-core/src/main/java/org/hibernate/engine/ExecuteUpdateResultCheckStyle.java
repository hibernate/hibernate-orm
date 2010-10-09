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
 *
 */
package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;

/**
 * For persistence operations (INSERT, UPDATE, DELETE) what style of determining
 * results (success/failure) is to be used.
 *
 * @author Steve Ebersole
 */
public class ExecuteUpdateResultCheckStyle implements Serializable {
	/**
	 * Do not perform checking.  Either user simply does not want checking, or is
	 * indicating a {@link java.sql.CallableStatement} execution in which the
	 * checks are being performed explicitly and failures are handled through
	 * propogation of {@link java.sql.SQLException}s.
	 */
	public static final ExecuteUpdateResultCheckStyle NONE = new ExecuteUpdateResultCheckStyle( "none" );
	/**
	 * Perform row-count checking.  Row counts are the int values returned by both
	 * {@link java.sql.PreparedStatement#executeUpdate()} and
	 * {@link java.sql.Statement#executeBatch()}.  These values are checked
	 * against some expected count.
	 */
	public static final ExecuteUpdateResultCheckStyle COUNT = new ExecuteUpdateResultCheckStyle( "rowcount" );
	/**
	 * Essentially the same as {@link #COUNT} except that the row count actually
	 * comes from an output parameter registered as part of a
	 * {@link java.sql.CallableStatement}.  This style explicitly prohibits
	 * statement batching from being used...
	 */
	public static final ExecuteUpdateResultCheckStyle PARAM = new ExecuteUpdateResultCheckStyle( "param" );

	private final String name;

	private ExecuteUpdateResultCheckStyle(String name) {
		this.name = name;
	}

	private Object readResolve() throws ObjectStreamException {
		Object resolved = parse( name );
		if ( resolved == null ) {
			throw new InvalidObjectException( "unknown result style [" + name + "]" );
		}
		return resolved;
	}

	public static ExecuteUpdateResultCheckStyle parse(String name) {
		if ( name.equals( NONE.name ) ) {
			return NONE;
		}
		else if ( name.equals( COUNT.name ) ) {
			return COUNT;
		}
		else if ( name.equals( PARAM.name ) ) {
			return PARAM;
		}
		else {
			return null;
		}
	}

	public static ExecuteUpdateResultCheckStyle determineDefault(String customSql, boolean callable) {
		if ( customSql == null ) {
			return COUNT;
		}
		else {
			return callable ? PARAM : COUNT;
		}
	}
}
