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
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Defines a contract for implementations that know how to convert a SQLException
 * into Hibernate's JDBCException hierarchy.  Inspired by Spring's
 * SQLExceptionTranslator.
 * <p/>
 * Implementations <b>must</b> have a constructor which takes a
 * {@link ViolatedConstraintNameExtracter} parameter.
 * <p/>
 * Implementations may implement {@link Configurable} if they need to perform
 * configuration steps prior to first use.
 *
 * @author Steve Ebersole
 * @see SQLExceptionConverterFactory
 */
public interface SQLExceptionConverter {
	/**
	 * Convert the given SQLException into Hibernate's JDBCException hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message      An optional error message.
	 * @return The resulting JDBCException.
	 * @see ConstraintViolationException, JDBCConnectionException, SQLGrammarException, LockAcquisitionException
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql);
}
