/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard implementation of JdbcCallParameterExtractor
 *
 * @author Steve Ebersole
 */
public class JdbcCallParameterExtractorImpl<T> implements JdbcCallParameterExtractor {
	private final String callableName;
	private final String parameterName;
	private final int parameterPosition;
	private final AllowableParameterType ormType;

	public JdbcCallParameterExtractorImpl(
			String callableName,
			String parameterName,
			int parameterPosition,
			AllowableParameterType ormType) {
		this.callableName = callableName;
		this.parameterName = parameterName;
		this.parameterPosition = parameterPosition;
		this.ormType = ormType;
	}

	@Override
	public String getParameterName() {
		return parameterName;
	}

	@Override
	public int getParameterPosition() {
		return parameterPosition;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			ExecutionContext executionContext) {
		if ( !BasicValuedExpressableType.class.isInstance( ormType ) ) {
			throw new NotYetImplementedFor6Exception(
					"Support for JDBC CallableStatement parameter extraction not yet supported for non-basic types"
			);
		}

		final boolean useNamed = shouldUseJdbcNamedParameters
				&& parameterName != null;

		final TypeConfiguration typeConfiguration = executionContext.getSession().getFactory().getTypeConfiguration();
		final JdbcValueExtractor valueExtractor = ( (BasicValuedExpressableType) ormType ).getBasicType()
				.getSqlTypeDescriptor()
				.getSqlExpressableType( ( BasicJavaDescriptor) ormType.getJavaTypeDescriptor(), typeConfiguration )
				.getJdbcValueExtractor();

		try {
			if ( useNamed ) {
				return (T) valueExtractor.extract( callableStatement, parameterName, executionContext );
			}
			else {
				return (T) valueExtractor.extract( callableStatement, parameterPosition, executionContext );
			}
		}
		catch (SQLException e) {
			throw executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to extract OUT/INOUT parameter value"
			);
		}
	}
}
