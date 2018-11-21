/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class InferredBasicValueResolution<J> implements BasicValueMapping.Resolution<J>, BasicValueMapper<J> {
	private final BasicType<J> basicType;

	private BasicJavaDescriptor<J> domainJtd;

	private BasicValueConverter valueConverter;
	private MutabilityPlan mutabilityPlan;

	public InferredBasicValueResolution(
			BasicTypeImpl<J> basicType,
			BasicJavaDescriptor<J> domainJtd,
			BasicValueConverter valueConverter,
			MutabilityPlan mutabilityPlan) {
		this.basicType = basicType;
		this.domainJtd = domainJtd;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;
	}


	@Override
	public BasicType getBasicType() {
		return basicType;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return this;
	}

	@Override
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return basicType.getSqlExpressableType();
	}

	@Override
	public BasicJavaDescriptor<?> getRelationalJavaDescriptor() {
		return getSqlExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return getSqlExpressableType().getSqlTypeDescriptor();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}
}
