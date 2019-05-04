/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import javax.persistence.TemporalType;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.internal.BindingTypeHelper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedExpressableType<J>
		extends ExpressableType<J>, AllowableParameterType<J>, AllowableFunctionReturnType<J> {

	@Override
	BasicJavaDescriptor<J> getJavaTypeDescriptor();

	default SqlTypeDescriptor getSqlTypeDescriptor() {
		if ( getSqlExpressableType() == null ) {
			return null;
		}
		return getSqlExpressableType().getSqlTypeDescriptor();
	}

	default SqlExpressableType getSqlExpressableType(TypeConfiguration typeConfiguration) {
		if ( getSqlExpressableType() == null ) {
			return null;
		}
		return getSqlTypeDescriptor().getSqlExpressableType( getJavaTypeDescriptor(), typeConfiguration );
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default int getNumberOfJdbcParametersNeeded() {
		return 1;
	}

	default AllowableParameterType resolveTemporalPrecision(
			TemporalType temporalType,
			TypeConfiguration typeConfiguration) {
		return BindingTypeHelper.INSTANCE.resolveTemporalPrecision( temporalType, this, typeConfiguration );
	}

	@Override
	default void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		jdbcValueCollector.collect( value, getSqlExpressableType(), null );
	}
}
