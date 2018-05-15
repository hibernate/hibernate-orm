/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.procedure.internal;

import java.sql.Types;
import javax.persistence.ParameterMode;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.spi.ParameterMemento;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class FunctionReturnImpl implements FunctionReturnImplementor {
	private final ProcedureCallImplementor procedureCall;
	private int jdbcTypeCode;

	private BasicValuedExpressableType ormType;

	public FunctionReturnImpl(ProcedureCallImplementor procedureCall, int jdbcTypeCode) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public FunctionReturnImpl(ProcedureCallImplementor procedureCall, BasicValuedExpressableType ormType) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = ormType.getBasicType().getSqlTypeDescriptor().getJdbcTypeCode();

		this.ormType = ormType;
	}


	public JdbcCallFunctionReturn toJdbcFunctionReturn(SharedSessionContractImplementor persistenceContext) {
		final AllowableParameterType ormType;
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
			ormType = typeConfiguration.getBasicTypeRegistry().getBasicType( javaTypeMapping.getJavaType() );
			parameterExtractor = new JdbcCallParameterExtractorImpl( procedureCall.getProcedureName(), null, 0, ormType );
			refCursorExtractor = null;
		}

		return new JdbcCallFunctionReturnImpl( getJdbcTypeCode(), ormType, parameterExtractor, refCursorExtractor );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	@Override
	public AllowableParameterType getHibernateType() {
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
		return ormType == null ? null : ormType.getJavaType();
	}

	@Override
	public void allowMultiValuedBinding() {
		// no-op
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return false;
	}

	@Override
	public ParameterMemento toMemento() {
		// todo (6.0) : do we need a FunctionReturnMemento?
		return new ParameterMemento() {
			@Override
			public QueryParameter toQueryParameter(SharedSessionContractImplementor session) {
				if ( ormType != null ) {
					return new FunctionReturnImpl( procedureCall, ormType );
				}
				else {
					return new FunctionReturnImpl( procedureCall, jdbcTypeCode );
				}
			}
		};
	}
}
