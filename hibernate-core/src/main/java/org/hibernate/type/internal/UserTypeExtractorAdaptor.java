/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeExtractorAdaptor<T> implements JdbcValueExtractor<T> {
	private final UserType userType;

	private final boolean canExtractFromCallable;

	public UserTypeExtractorAdaptor(UserType userType) {
		this.userType = userType;

		this.canExtractFromCallable = userType instanceof ProcedureParameterExtractionAware;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(
			ResultSet resultSet,
			int jdbcParameterPosition,
			ExecutionContext executionContext) throws SQLException {
		return (T) userType.nullSafeGet( resultSet, jdbcParameterPosition, executionContext.getSession() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(
			CallableStatement statement,
			int jdbcParameterPosition,
			ExecutionContext executionContext) throws SQLException {
		if ( canExtractFromCallable ) {
			return (T) ( (ProcedureParameterExtractionAware) userType ).extract(
					statement,
					jdbcParameterPosition,
					executionContext.getSession()
			);
		}

		throw new UnsupportedOperationException(
				"UserType [" + userType.getClass().getName() + "] does not implement "
						+ ProcedureParameterExtractionAware.class.getName()
						+ ", cannot extract values from CallableStatement"
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(
			CallableStatement statement,
			String jdbcParameterName,
			ExecutionContext executionContext) throws SQLException {
		if ( canExtractFromCallable ) {
			return (T) ( (ProcedureParameterExtractionAware) userType ).extract(
					statement,
					jdbcParameterName,
					executionContext.getSession()
			);
		}

		throw new UnsupportedOperationException(
				"UserType [" + userType.getClass().getName() + "] does not implement "
						+ ProcedureParameterExtractionAware.class.getName()
						+ ", cannot extract values from CallableStatement"
		);
	}
}
