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
import org.hibernate.type.BasicType;
import org.hibernate.type.ProcedureParameterExtractionAware;

/**
 * @author Steve Ebersole
 */
public class BasicTypeExtractorAdaptor<T> implements JdbcValueExtractor<T> {
	private final BasicType basicType;

	private final boolean canExtractFromCallable;

	public BasicTypeExtractorAdaptor(BasicType basicType) {
		this.basicType = basicType;

		this.canExtractFromCallable = basicType instanceof ProcedureParameterExtractionAware;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(
			ResultSet resultSet,
			int jdbcParameterPosition,
			ExecutionContext executionContext) throws SQLException {
		return (T) basicType.nullSafeGet( resultSet, jdbcParameterPosition, executionContext.getSession() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(
			CallableStatement statement,
			int jdbcParameterPosition,
			ExecutionContext executionContext) throws SQLException {
		if ( canExtractFromCallable ) {
			return (T) ( (ProcedureParameterExtractionAware) basicType ).extract(
					statement,
					jdbcParameterPosition,
					executionContext.getSession()
			);
		}

		throw new UnsupportedOperationException(
				"BasicType [" + basicType.getClass().getName() + "] does not implement "
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
			return (T) ( (ProcedureParameterExtractionAware) basicType ).extract(
					statement,
					jdbcParameterName,
					executionContext.getSession()
			);
		}

		throw new UnsupportedOperationException(
				"BasicType [" + basicType.getClass().getName() + "] does not implement "
						+ ProcedureParameterExtractionAware.class.getName()
						+ ", cannot extract values from CallableStatement"
		);
	}
}
