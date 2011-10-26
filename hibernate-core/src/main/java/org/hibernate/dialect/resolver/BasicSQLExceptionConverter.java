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
package org.hibernate.dialect.resolver;
import java.sql.SQLException;

import org.jboss.logging.Logger;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLStateConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A helper to centralize conversion of {@link java.sql.SQLException}s to {@link org.hibernate.JDBCException}s.
 *
 * @author Steve Ebersole
 */
public class BasicSQLExceptionConverter {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, BasicSQLExceptionConverter.class.getName() );
	public static final BasicSQLExceptionConverter INSTANCE = new BasicSQLExceptionConverter();
	public static final String MSG = LOG.unableToQueryDatabaseMetadata();

	private static final SQLStateConverter CONVERTER = new SQLStateConverter( new ConstraintNameExtracter() );

	/**
	 * Perform a conversion.
	 *
	 * @param sqlException The exception to convert.
	 * @return The converted exception.
	 */
	public JDBCException convert(SQLException sqlException) {
		return CONVERTER.convert( sqlException, MSG, null );
	}

	private static class ConstraintNameExtracter implements ViolatedConstraintNameExtracter {
		/**
		 * {@inheritDoc}
		 */
		public String extractConstraintName(SQLException sqle) {
			return "???";
		}
	}
}
