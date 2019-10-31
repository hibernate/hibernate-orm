/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EnumeratedValueResolution<E extends Enum<E>> implements BasicValue.Resolution<E> {
	private final CustomType enumTypeMapping;
	private final JavaTypeDescriptor<E> domainJtd;
	private final JavaTypeDescriptor<?> jdbcJtd;
	private final SqlTypeDescriptor std;
	private final EnumValueConverter<E,?> valueConverter;

	public EnumeratedValueResolution(
			CustomType enumTypeMapping,
			JavaTypeDescriptor<E> domainJtd,
			JavaTypeDescriptor<?> jdbcJtd,
			SqlTypeDescriptor std,
			EnumValueConverter<E, ?> valueConverter) {
		this.enumTypeMapping = enumTypeMapping;
		this.domainJtd = domainJtd;
		this.jdbcJtd = jdbcJtd;
		this.std = std;
//		this.valueConverter = valueConverter;
		this.valueConverter = null;
	}

	@Override
	public BasicType getResolvedBasicType() {
		return enumTypeMapping;
	}

	@Override
	public JavaTypeDescriptor<E> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
		return jdbcJtd;
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return std;
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<E> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
