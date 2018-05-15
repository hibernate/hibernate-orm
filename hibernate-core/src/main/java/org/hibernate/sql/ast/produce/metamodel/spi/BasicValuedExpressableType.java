/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import javax.persistence.TemporalType;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedExpressableType<J>
		extends ExpressableType<J>, AllowableParameterType<J>, AllowableFunctionReturnType<J> {
	BasicType<J> getBasicType();

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	BasicJavaDescriptor<J> getJavaTypeDescriptor();

	default SqlTypeDescriptor getSqlTypeDescriptor() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	default int getNumberOfJdbcParametersNeeded() {
		return 1;
	}

	default AllowableParameterType resolveTemporalPrecision(
			TemporalType temporalType,
			TypeConfiguration typeConfiguration) {
		return getBasicType().resolveTemporalPrecision( temporalType, typeConfiguration );
	}

	@Override
	default void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		jdbcValueCollector.collect(
				value,
				getBasicType().getSqlExpressableType( session.getFactory().getTypeConfiguration() ),
				null
		);
	}
}
