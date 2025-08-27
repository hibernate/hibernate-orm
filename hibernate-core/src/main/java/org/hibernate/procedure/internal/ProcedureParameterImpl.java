/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import java.util.Locale;
import java.util.Objects;

import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ParameterTypeException;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.type.BindableType;
import org.hibernate.type.OutputableType;
import org.hibernate.type.internal.BindingTypeHelper;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterRegistrationImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.BasicType;
import org.hibernate.type.ProcedureParameterNamedBinder;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends AbstractQueryParameter<T> implements ProcedureParameterImplementor<T> {

	private final String name;
	private final Integer position;
	private final ParameterMode mode;
	private final Class<T> javaType;

	/**
	 * Used for named Query parameters
	 */
	public ProcedureParameterImpl(
			String name,
			ParameterMode mode,
			Class<T> javaType,
			BindableType<T> hibernateType) {
		super( false, hibernateType );
		this.name = name;
		this.position = null;
		this.mode = mode;
		this.javaType = javaType;
	}

	/**
	 * Used for ordinal Query parameters
	 */
	public ProcedureParameterImpl(
			Integer position,
			ParameterMode mode,
			Class<T> javaType,
			BindableType<T> hibernateType) {
		super( false, hibernateType );
		this.name = null;
		this.position = position;
		this.mode = mode;
		this.javaType = javaType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public Class<T> getParameterType() {
		return javaType;
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> isNamed()
				? new ProcedureParameterImpl<T>( getName(), getMode(), javaType, getHibernateType() )
				: new ProcedureParameterImpl<T>( getPosition(), getMode(), javaType, getHibernateType() );
	}

	@Override
	public JdbcCallParameterRegistration toJdbcParameterRegistration(
			int startIndex,
			ProcedureCallImplementor<?> procedureCall) {
		final QueryParameterBinding<T> binding = procedureCall.getParameterBindings().getBinding( this );
		final boolean isNamed = procedureCall.getParameterStrategy() == ParameterStrategy.NAMED && this.name != null;
		final SharedSessionContractImplementor session = procedureCall.getSession();

		final OutputableType<T> typeToUse = (OutputableType<T>)
				BindingTypeHelper.resolveTemporalPrecision(
						binding == null ? null : binding.getExplicitTemporalPrecision(),
						getBindableType( binding ),
						session.getFactory().getQueryEngine().getCriteriaBuilder()
				);

		final String jdbcParamName;
		final JdbcParameterBinder parameterBinder;
		final JdbcCallRefCursorExtractorImpl refCursorExtractor;
		final JdbcCallParameterExtractorImpl<T> parameterExtractor;
		final ExtractedDatabaseMetaData databaseMetaData =
				session.getFactory().getJdbcServices().getJdbcEnvironment()
						.getExtractedDatabaseMetaData();
		final boolean passProcedureParameterNames =
				session.getFactory().getSessionFactoryOptions().isPassProcedureParameterNames();
		switch ( mode ) {
			case REF_CURSOR:
				jdbcParamName = this.name != null && databaseMetaData.supportsNamedParameters() && passProcedureParameterNames ? this.name : null;
				refCursorExtractor = new JdbcCallRefCursorExtractorImpl( startIndex );
				parameterBinder = null;
				parameterExtractor = null;
				break;
			case IN:
				jdbcParamName = getJdbcParamName( procedureCall, isNamed, passProcedureParameterNames, typeToUse, databaseMetaData );
				validateBindableType( typeToUse, startIndex );
				parameterBinder = getParameterBinder( typeToUse, jdbcParamName );
				parameterExtractor = null;
				refCursorExtractor = null;
				break;
			case INOUT:
				jdbcParamName = getJdbcParamName( procedureCall, isNamed, passProcedureParameterNames, typeToUse, databaseMetaData );
				validateBindableType( typeToUse, startIndex );
				parameterBinder = getParameterBinder( typeToUse, jdbcParamName );
				parameterExtractor = new JdbcCallParameterExtractorImpl<>( procedureCall.getProcedureName(), jdbcParamName, startIndex, typeToUse );
				refCursorExtractor = null;
				break;
			default:
				jdbcParamName = getJdbcParamName( procedureCall, isNamed, passProcedureParameterNames, typeToUse, databaseMetaData );
				validateBindableType( typeToUse, startIndex );
				parameterBinder = null;
				parameterExtractor = new JdbcCallParameterExtractorImpl<>( procedureCall.getProcedureName(), jdbcParamName, startIndex, typeToUse );
				refCursorExtractor = null;
				break;
		}

		return new JdbcCallParameterRegistrationImpl( jdbcParamName, startIndex, mode, typeToUse, parameterBinder, parameterExtractor, refCursorExtractor );
	}

	private BindableType<T> getBindableType(QueryParameterBinding<T> binding) {
		if ( getHibernateType() != null ) {
			return getHibernateType();
		}
		else if ( binding != null ) {
			//noinspection unchecked
			return (BindableType<T>) binding.getBindType();
		}
		else {
			return null;
		}
	}

	private String getJdbcParamName(
			ProcedureCallImplementor<?> procedureCall,
			boolean isNamed,
			boolean passProcedureParameterNames,
			OutputableType<T> typeToUse,
			ExtractedDatabaseMetaData databaseMetaData) {
		return isNamed && passProcedureParameterNames
			&& canDoNameParameterBinding( typeToUse, procedureCall, databaseMetaData )
				? this.name
				: null;
	}

	private void validateBindableType(BindableType<T> bindableType, int startIndex) {
		if ( bindableType == null ) {
			throw new ParameterTypeException(
					String.format(
							Locale.ROOT,
							"Could not determine ProcedureCall parameter bind type - %s (%s)",
							this.name != null ? this.name : this.position,
							startIndex
					)
			);
		}
	}

	private JdbcParameterBinder getParameterBinder(BindableType<T> typeToUse, String name) {
		if ( typeToUse == null ) {
			throw new ParameterTypeException(
					String.format(
							Locale.ROOT,
							"Cannot determine the bindable type for procedure parameter %s (%s)",
							this.name != null ? this.name : this.position,
							name
					)
			);
		}
		else if ( typeToUse instanceof BasicType<?> ) {
			return new JdbcParameterImpl( (BasicType<T>) typeToUse );
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	private boolean canDoNameParameterBinding(
			BindableType<?> hibernateType,
			ProcedureCallImplementor<?> procedureCall,
			ExtractedDatabaseMetaData databaseMetaData) {
		return procedureCall.getFunctionReturn() == null
			&& databaseMetaData.supportsNamedParameters()
			&& hibernateType instanceof ProcedureParameterNamedBinder
			&& ( (ProcedureParameterNamedBinder<?>) hibernateType ).canDoSetting();
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, position, mode );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null ) {
			return false;
		}
		if ( !(o instanceof ProcedureParameterImpl<?> that) ) {
			return false;
		}
		return Objects.equals( name, that.name )
			&& Objects.equals( position, that.position )
			&& mode == that.mode;
	}

	@Override
	public String toString() {
		return position == null ? name : position.toString();
	}
}
