/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.OutputableType;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class FunctionReturnImpl<T> implements FunctionReturnImplementor<T> {
	private final ProcedureCallImplementor<T> procedureCall;
	private final int sqlTypeCode;

	private OutputableType<T> ormType;

	public FunctionReturnImpl(ProcedureCallImplementor<T> procedureCall, int sqlTypeCode) {
		this.procedureCall = procedureCall;
		this.sqlTypeCode = sqlTypeCode;
	}

	public FunctionReturnImpl(ProcedureCallImplementor<T> procedureCall, OutputableType<T> ormType) {
		this.procedureCall = procedureCall;
		this.sqlTypeCode = ormType.getJdbcType().getDefaultSqlTypeCode();
		this.ormType = ormType;
	}

	@Override
	public JdbcCallFunctionReturn toJdbcFunctionReturn(SharedSessionContractImplementor persistenceContext) {
		final OutputableType<T> ormType;
		final JdbcCallRefCursorExtractorImpl refCursorExtractor;
		final JdbcCallParameterExtractorImpl<T> parameterExtractor;

		if ( getJdbcTypeCode() == Types.REF_CURSOR ) {
			refCursorExtractor = new JdbcCallRefCursorExtractorImpl( 1 );
			ormType = null;
			parameterExtractor = null;
		}
		else {
			if ( this.ormType != null ) {
				ormType = this.ormType;
			}
			else {
				final TypeConfiguration typeConfiguration = persistenceContext.getFactory().getTypeConfiguration();
				final JdbcType sqlTypeDescriptor = typeConfiguration.getJdbcTypeRegistry().getDescriptor(
						getJdbcTypeCode()
				);
				final JavaType<?> javaTypeMapping = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping(
						null,
						null,
						typeConfiguration
				);
				//noinspection unchecked
				ormType = (OutputableType<T>) typeConfiguration.standardBasicTypeForJavaType( javaTypeMapping.getJavaTypeClass() );
			}
			parameterExtractor = new JdbcCallParameterExtractorImpl<>( procedureCall.getProcedureName(), null, 1, ormType );
			refCursorExtractor = null;
		}

		return new JdbcCallFunctionReturnImpl( ormType, parameterExtractor, refCursorExtractor );
	}

	@Override
	public int getJdbcTypeCode() {
		return sqlTypeCode;
	}

	@Override
	public BindableType<T> getHibernateType() {
		return ormType;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return 1;
	}

	@Override
	public ParameterMode getMode() {
		return ParameterMode.OUT;
	}

	@Override
	public Class getParameterType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disallowMultiValuedBinding() {
		// no-op
	}

	@Override
	public void applyAnticipatedType(BindableType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return false;
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> {
			if ( ormType != null ) {
				return new FunctionReturnImpl<>( procedureCall, ormType );
			}
			else {
				return new FunctionReturnImpl<>( procedureCall, sqlTypeCode );
			}
		};
	}
}
