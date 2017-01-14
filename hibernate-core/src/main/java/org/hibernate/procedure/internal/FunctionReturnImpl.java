/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.procedure.internal;

import java.sql.Types;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ParameterBindImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class FunctionReturnImpl implements FunctionReturnImplementor {
	private final ProcedureCallImplementor procedureCall;
	private int jdbcTypeCode;
	private Type ormType;

	public FunctionReturnImpl(ProcedureCallImplementor procedureCall, int jdbcTypeCode) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public FunctionReturnImpl(ProcedureCallImplementor procedureCall, Type ormType) {
		this.procedureCall = procedureCall;
		setHibernateType( ormType );
	}

	@Override
	public ProcedureCallImplementor getProcedureCall() {
		return procedureCall;
	}

	public JdbcCallParameterRegistration toJdbcCallParameterRegistration(SharedSessionContractImplementor persistenceContext) {
		final Type ormType;
		final JdbcCallRefCursorExtractorImpl refCursorExtractor;
		final JdbcCallParameterExtractorImpl parameterExtractor;

		if ( getJdbcTypeCode() == Types.REF_CURSOR ) {
			refCursorExtractor = new JdbcCallRefCursorExtractorImpl( null, 0 );
			ormType = null;
			parameterExtractor = null;
		}
		else {

			final TypeConfiguration typeConfiguration = persistenceContext.getFactory().getMetamodel().getTypeConfiguration();
			final SqlTypeDescriptor sqlTypeDescriptor = typeConfiguration.getSqlTypeDescriptorRegistry()
					.getDescriptor( getJdbcTypeCode() );
			final JavaTypeDescriptor javaTypeMapping = sqlTypeDescriptor
					.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
			ormType = typeConfiguration.heuristicType( javaTypeMapping.getTypeName() );
			parameterExtractor = new JdbcCallParameterExtractorImpl( procedureCall.getProcedureName(), null, 0, ormType );
			refCursorExtractor = null;
		}

		return new JdbcCallFunctionReturnImpl( getJdbcTypeCode(), ormType, parameterExtractor, refCursorExtractor );
	}

	@Override
	public ParameterBindImplementor getBind() {
		throw new HibernateException( "Function return does not define binding" );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	@Override
	public void setHibernateType(Type ormType) {
		assert ormType.sqlTypes().length == 1;
		this.ormType = ormType;
		this.jdbcTypeCode = ormType.sqlTypes()[0];
	}

	@Override
	public Type getHibernateType() {
		return ormType;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return 0;
	}

	@Override
	public ParameterMode getMode() {
		return ParameterMode.OUT;
	}

	@Override
	public Class getParameterType() {
		return null;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return false;
	}

	@Override
	public void bindValue(Object value) {
		throw new HibernateException( "Function return does not define binding" );
	}

	@Override
	public void bindValue(Object value, TemporalType explicitTemporalType) {
		throw new HibernateException( "Function return does not define binding" );
	}

	@Override
	public void enablePassingNulls(boolean enabled) {
		throw new HibernateException( "enablePassingNulls is not valid on a function return" );
	}
}
