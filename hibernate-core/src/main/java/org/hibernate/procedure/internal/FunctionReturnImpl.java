/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.procedure.internal;

import java.sql.Types;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.OutputableType;
import org.hibernate.query.BindableType;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class FunctionReturnImpl<T> implements FunctionReturnImplementor<T> {
	private final ProcedureCallImplementor<T> procedureCall;
	private final int jdbcTypeCode;

	private OutputableType<T> ormType;

	public FunctionReturnImpl(ProcedureCallImplementor<T> procedureCall, int jdbcTypeCode) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public FunctionReturnImpl(ProcedureCallImplementor<T> procedureCall, OutputableType<T> ormType) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = ormType.getJdbcTypeDescriptor().getJdbcTypeCode();
		this.ormType = ormType;
	}

	@Override
	public JdbcCallFunctionReturn toJdbcFunctionReturn(SharedSessionContractImplementor persistenceContext) {
		final BindableType<T> ormType;
		final JdbcCallRefCursorExtractorImpl refCursorExtractor;
		final JdbcCallParameterExtractorImpl<T> parameterExtractor;

		if ( getJdbcTypeCode() == Types.REF_CURSOR ) {
			refCursorExtractor = new JdbcCallRefCursorExtractorImpl( null, 1 );
			ormType = null;
			parameterExtractor = null;
		}
		else {
			final TypeConfiguration typeConfiguration = persistenceContext.getFactory().getMetamodel().getTypeConfiguration();
			final JdbcType sqlTypeDescriptor = typeConfiguration.getJdbcTypeDescriptorRegistry()
					.getDescriptor( getJdbcTypeCode() );
			final BasicJavaType<?> javaTypeMapping = sqlTypeDescriptor
					.getJdbcRecommendedJavaTypeMapping( null, null, typeConfiguration );
			//noinspection unchecked
			ormType = (BindableType<T>) typeConfiguration.standardBasicTypeForJavaType( javaTypeMapping.getJavaTypeClass() );
			parameterExtractor = new JdbcCallParameterExtractorImpl<>( procedureCall.getProcedureName(), null, 1, ormType );
			refCursorExtractor = null;
		}

		return new JdbcCallFunctionReturnImpl( ormType, parameterExtractor, refCursorExtractor );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcTypeCode;
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

//		return ormType == null ? null : ormType.getJavaType();
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void disallowMultiValuedBinding() {
		// no-op
	}

	@Override
	public void applyAnticipatedType(BindableType type) {
		throw new NotYetImplementedFor6Exception( getClass() );
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
				return new FunctionReturnImpl<>( procedureCall, jdbcTypeCode );
			}
		};
	}
}
