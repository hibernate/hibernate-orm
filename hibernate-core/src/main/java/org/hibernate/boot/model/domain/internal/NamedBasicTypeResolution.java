/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class NamedBasicTypeResolution<J> implements BasicValueMapping.Resolution<J>, BasicValueMapper<J> {
	private final BasicJavaDescriptor<J> domainJtd;

	private final BasicType basicType;
	private final SqlExpressableType sqlExpressableType;

	private final BasicValueConverter valueConverter;
	private final MutabilityPlan<J> mutabilityPlan;

	public NamedBasicTypeResolution(
			BasicJavaDescriptor<J> domainJtd,
			BasicType basicType,
			BasicValueConverter valueConverter,
			MutabilityPlan<J> mutabilityPlan,
			ResolutionContext resolutionContext) {
		this.domainJtd = domainJtd;

		this.basicType = basicType;
		this.sqlExpressableType = basicType.getSqlTypeDescriptor().getSqlExpressableType(
				basicType.getJavaTypeDescriptor(),
				resolutionContext.getBootstrapContext().getTypeConfiguration()
		);

		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;
	}

	@Override
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return sqlExpressableType;
	}

	@Override
	public BasicJavaDescriptor<?> getRelationalJavaDescriptor() {
		return basicType.getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return basicType.getSqlTypeDescriptor();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public BasicType getBasicType() {
		return basicType;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return this;
	}
}
