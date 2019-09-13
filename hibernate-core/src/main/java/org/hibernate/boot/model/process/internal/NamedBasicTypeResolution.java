/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class NamedBasicTypeResolution<J> implements BasicValue.Resolution<J> {
	private final JavaTypeDescriptor<J> domainJtd;

	private final BasicType basicType;

	private final BasicValueConverter valueConverter;
	private final MutabilityPlan<J> mutabilityPlan;

	public NamedBasicTypeResolution(
			JavaTypeDescriptor<J> domainJtd,
			BasicType basicType,
			BasicValueConverter valueConverter,
			MutabilityPlan<J> mutabilityPlan,
			MetadataBuildingContext context) {
		this.domainJtd = domainJtd;

		this.basicType = basicType;

		this.valueConverter = valueConverter;
		if ( mutabilityPlan == null ) {
			this.mutabilityPlan = domainJtd.getMutabilityPlan();
		}
		else {
			this.mutabilityPlan = mutabilityPlan;
		}
	}

	@Override
	public BasicType getResolvedBasicType() {
		return basicType;
	}

	@Override
	public JavaTypeDescriptor<J> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
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
}
