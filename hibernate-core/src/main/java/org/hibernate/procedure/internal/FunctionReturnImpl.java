/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.procedure.internal;

import java.sql.Types;
import jakarta.persistence.ParameterMode;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.AllowableOutputParameterType;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.sql.exec.internal.JdbcCallFunctionReturnImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class FunctionReturnImpl implements FunctionReturnImplementor {
	private final ProcedureCallImplementor procedureCall;
	private int jdbcTypeCode;

	private AllowableOutputParameterType<?> ormType;

	public FunctionReturnImpl(ProcedureCallImplementor procedureCall, int jdbcTypeCode) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public FunctionReturnImpl(ProcedureCallImplementor procedureCall, AllowableOutputParameterType ormType) {
		this.procedureCall = procedureCall;
		this.jdbcTypeCode = ormType.getJdbcTypeDescriptor().getJdbcTypeCode();

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
			final JdbcTypeDescriptor sqlTypeDescriptor = typeConfiguration.getJdbcTypeDescriptorRegistry()
					.getDescriptor( getJdbcTypeCode() );
			final BasicJavaTypeDescriptor javaTypeMapping = sqlTypeDescriptor
					.getJdbcRecommendedJavaTypeMapping( null, null, typeConfiguration );
			ormType = typeConfiguration.standardBasicTypeForJavaType( javaTypeMapping.getJavaType().getClass() );
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

//		return ormType == null ? null : ormType.getJavaType();
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void disallowMultiValuedBinding() {
		// no-op
	}

	@Override
	public void applyAnticipatedType(AllowableParameterType type) {
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
				return new FunctionReturnImpl( procedureCall, ormType );
			}
			else {
				return new FunctionReturnImpl( procedureCall, jdbcTypeCode );
			}
		};
	}
}
