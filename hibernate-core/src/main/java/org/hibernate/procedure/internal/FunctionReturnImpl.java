package org.hibernate.procedure.internal;

import org.hibernate.type.internal.TemporalMappingValidator;
import org.hibernate.type.internal.TemporalCallableType;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;

import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.type.BindableType;
import org.hibernate.type.OutputableType;
import org.hibernate.type.BasicType;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl.RefCurserJdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl.RegularJdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
class FunctionReturnImpl<T> implements FunctionReturnImplementor<T> {

	private final ProcedureCallImplementor<?> procedureCall;
	private final int sqlTypeCode;
	private final OutputableType<T> ormType;

	FunctionReturnImpl(ProcedureCallImplementor<?> procedureCall, int sqlTypeCode) {
		this.procedureCall = procedureCall;
		this.sqlTypeCode = sqlTypeCode;
		this.ormType = resolveOrmType( procedureCall, sqlTypeCode );
	}

	FunctionReturnImpl(ProcedureCallImplementor<?> procedureCall, OutputableType<T> ormType) {
		this.procedureCall = procedureCall;
		this.sqlTypeCode = ormType.getJdbcType().getDefaultSqlTypeCode();
		this.ormType = ormType;
	}

	@SuppressWarnings("unchecked")
	private static <T> OutputableType<T> resolveOrmType(ProcedureCallImplementor<?> procedureCall, int sqlTypeCode) {
		final var typeConfiguration = procedureCall.getSession().getFactory().getTypeConfiguration();
		final var javaType =
				typeConfiguration.getJdbcTypeRegistry().getDescriptor( sqlTypeCode )
						.getRecommendedJavaType( null, null, typeConfiguration );
		return (OutputableType<T>) typeConfiguration.standardBasicTypeForJavaType( javaType.getJavaTypeClass() );
	}

	@Override
	@Nonnull
	public JdbcCallFunctionReturn toJdbcFunctionReturn(@Nonnull SharedSessionContractImplementor persistenceContext) {
		if ( getJdbcTypeCode() == Types.REF_CURSOR ) {
			return new RefCurserJdbcCallFunctionReturnImpl( new JdbcCallRefCursorExtractorImpl( 1 ) );
		}
		else {
			if ( persistenceContext.getFactory().getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled()
					&& ormType instanceof BasicType<?> basic ) {
				TemporalMappingValidator.validate(
						persistenceContext.getFactory().getJdbcServices().getDialect(), basic,
						procedureCall.getProcedureName() + " function return", persistenceContext.getFactory().getProperties() );
			}
			final var callableType = TemporalCallableType.resolve( ormType, persistenceContext,
					procedureCall.getProcedureName() + " function return", ParameterMode.OUT, true, Access.INDEXED );
			return new RegularJdbcCallFunctionReturnImpl(
					callableType,
					new JdbcCallParameterExtractorImpl<T>(
							procedureCall.getProcedureName(),
							null,
							1,
							callableType
					)
			);
		}
	}

	@Override
	public int getJdbcTypeCode() {
		return sqlTypeCode;
	}

	@Override
	@Nonnull
	public BindableType<T> getHibernateType() {
		return ormType;
	}

	@Override
	@Nullable
	public String getName() {
		return null;
	}

	@Override
	@Nonnull
	public Integer getPosition() {
		return 1;
	}

	@Override
	@Nonnull
	public ParameterMode getMode() {
		return ParameterMode.OUT;
	}

	@Override
	@Nonnull
	public Class<T> getParameterType() {
		return ormType.getJavaType();
	}

	@Override
	public void disallowMultiValuedBinding() {
		// no-op
	}

	@Override
	public void applyAnticipatedType(@Nullable BindableType<?> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return false;
	}

	@Override
	@Nonnull
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> new FunctionReturnImpl<>( procedureCall, ormType );
	}
}
