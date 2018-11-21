/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SimpleBasicValueMapper<J> implements BasicValueMapper<J> {
	private final BasicJavaDescriptor<J> domainJtd;
	private final SqlExpressableType jdbcType;
	private final BasicValueConverter valueConverter;
	private final MutabilityPlan<J> mutabilityPlan;

	public SimpleBasicValueMapper(
			BasicJavaDescriptor<J> domainJtd,
			SqlExpressableType jdbcType,
			BasicValueConverter valueConverter,
			MutabilityPlan<J> mutabilityPlan) {
		this.domainJtd = domainJtd;
		this.jdbcType = jdbcType;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;
	}

	@Override
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return jdbcType;
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
