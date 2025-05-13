/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.type.BindableType;
import org.hibernate.type.OutputableType;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl.RefCurserJdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl.RegularJdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
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
		if ( getJdbcTypeCode() == Types.REF_CURSOR ) {
			return new RefCurserJdbcCallFunctionReturnImpl( new JdbcCallRefCursorExtractorImpl( 1 ) );
		}
		else {
			return new RegularJdbcCallFunctionReturnImpl(
					getOrmType( persistenceContext ),
					new JdbcCallParameterExtractorImpl<T>(
							procedureCall.getProcedureName(),
							null,
							1,
							getOrmType( persistenceContext )
					)
			);
		}
	}

	private OutputableType<T> getOrmType(SharedSessionContractImplementor persistenceContext) {
		if ( ormType != null ) {
			return ormType;
		}
		else {
			final TypeConfiguration typeConfiguration = persistenceContext.getFactory().getTypeConfiguration();
			final JavaType<?> javaType =
					typeConfiguration.getJdbcTypeRegistry().getDescriptor( getJdbcTypeCode() )
							.getJdbcRecommendedJavaTypeMapping( null, null, typeConfiguration );
			final BasicType<?> basicType =
					typeConfiguration.standardBasicTypeForJavaType( javaType.getJavaTypeClass() );
			//noinspection unchecked
			return (OutputableType<T>) basicType;
		}
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
	public Class<T> getParameterType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disallowMultiValuedBinding() {
		// no-op
	}

	@Override
	public void applyAnticipatedType(BindableType<?> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return false;
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> ormType != null
				? new FunctionReturnImpl<T>( procedureCall, ormType )
				: new FunctionReturnImpl<T>( procedureCall, sqlTypeCode );
	}
}
