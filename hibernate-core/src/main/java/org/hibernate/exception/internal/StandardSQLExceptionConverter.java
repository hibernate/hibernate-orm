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
package org.hibernate.exception.internal;

import java.sql.SQLException;
import java.util.ArrayList;

import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;

/**
 * @author Steve Ebersole
 */
public class StandardSQLExceptionConverter implements SQLExceptionConverter {
	private ArrayList<SQLExceptionConversionDelegate> delegates = new ArrayList<SQLExceptionConversionDelegate>();

	public void addDelegate(SQLExceptionConversionDelegate delegate) {
		if ( delegate != null ) {
			this.delegates.add( delegate );
		}
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		for ( SQLExceptionConversionDelegate delegate : delegates ) {
			final JDBCException jdbcException = delegate.convert( sqlException, message, sql );
			if ( jdbcException != null ) {
				return jdbcException;
			}
		}
		return new GenericJDBCException( message, sqlException, sql );
	}
}
