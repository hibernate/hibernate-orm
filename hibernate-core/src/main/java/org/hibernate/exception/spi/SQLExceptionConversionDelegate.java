/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.exception.spi;

import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * Allow a {@link SQLExceptionConverter} to work by chaining together multiple such delegates.  The main
 * difference between a delegate and a full-fledged converter is that a delegate may return {@code null}.
 *
 * @author Steve Ebersole
 */
public interface SQLExceptionConversionDelegate {
	/**
	 * Convert the given SQLException into the Hibernate {@link org.hibernate.JDBCException} hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message An (optional) error message.
	 * @param sql The {@literal SQL} statement, if one, being performed when the exception occurred.
	 *
	 * @return The resulting JDBCException, can be {@code null}
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql);

}
