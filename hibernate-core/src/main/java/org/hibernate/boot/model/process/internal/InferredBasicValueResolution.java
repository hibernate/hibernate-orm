/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class InferredBasicValueResolution<J> implements BasicValue.Resolution<J> {
	private final BasicType<J> basicType;

	private JavaTypeDescriptor<J> domainJtd;
	private JavaTypeDescriptor<J> relationalJtd;
	private SqlTypeDescriptor relationalStd;

	private BasicValueConverter valueConverter;
	private MutabilityPlan mutabilityPlan;

	public InferredBasicValueResolution(
			BasicType<J> basicType,
			JavaTypeDescriptor<J> domainJtd,
			JavaTypeDescriptor<J> relationalJtd,
			SqlTypeDescriptor relationalStd,
			BasicValueConverter valueConverter,
			MutabilityPlan mutabilityPlan) {
		this.basicType = basicType;
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.relationalStd = relationalStd;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;
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
		return relationalJtd;
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return relationalStd;
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
