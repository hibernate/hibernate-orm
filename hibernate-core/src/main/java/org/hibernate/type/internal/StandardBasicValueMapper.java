/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.annotations.Type;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * Standard implementation of BasicValueMapper used for mappings
 * that do not use the legacy (deprecated) named-type approach as
 * part of domain mapping (e.g. {@link Type#type()}
 *
 * @author Steve Ebersole
 */
public class StandardBasicValueMapper<J> implements BasicValueMapper<J> {
	private final BasicJavaDescriptor domainJtd;
	private final SqlExpressableType sqlExpressableType;
	private final BasicValueConverter converter;
	private final MutabilityPlan mutabilityPlan;

	public StandardBasicValueMapper(
			BasicJavaDescriptor domainJtd,
			SqlExpressableType sqlExpressableType,
			BasicValueConverter converter,
			MutabilityPlan mutabilityPlan) {
		this.domainJtd = domainJtd;
		this.sqlExpressableType = sqlExpressableType;
		this.converter = converter;
		this.mutabilityPlan = mutabilityPlan;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return sqlExpressableType;
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return converter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}
}
