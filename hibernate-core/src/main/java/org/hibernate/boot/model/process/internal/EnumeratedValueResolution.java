/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Steve Ebersole
 */
public class EnumeratedValueResolution<E extends Enum<E>> implements BasicValue.Resolution<E> {
	private final CustomType<Object> enumTypeMapping;
	private final JavaType<E> domainJtd;
	private final JavaType<?> jdbcJtd;
	private final JdbcType jdbcType;
	private final EnumValueConverter<E,?> valueConverter;

	public EnumeratedValueResolution(
			CustomType<Object> enumTypeMapping,
			JavaType<E> domainJtd,
			JavaType<?> jdbcJtd,
			JdbcType jdbcType,
			EnumValueConverter<E, ?> valueConverter) {
		this.enumTypeMapping = enumTypeMapping;
		this.domainJtd = domainJtd;
		this.jdbcJtd = jdbcJtd;
		this.jdbcType = jdbcType;
		this.valueConverter = valueConverter;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return enumTypeMapping;
	}

	@Override
	public BasicType getLegacyResolvedBasicType() {
		return enumTypeMapping;
	}

	@Override
	public JavaType<E> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public JavaType<?> getRelationalJavaDescriptor() {
		return jdbcJtd;
	}

	@Override
	public JdbcType getJdbcTypeDescriptor() {
		return jdbcType;
	}

	@Override
	public EnumValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<E> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
