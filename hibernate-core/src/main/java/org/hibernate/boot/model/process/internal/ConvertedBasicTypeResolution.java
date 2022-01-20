/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Steve Ebersole
 */
public class ConvertedBasicTypeResolution<J> implements BasicValue.Resolution<J> {
	private final ConvertedBasicType basicType;
	private final ValueConverterTypeAdapter adapted;

	public ConvertedBasicTypeResolution(
			ConvertedBasicType basicType,
			JdbcTypeIndicators stdIndicators) {
		this.basicType = basicType;

		final BasicValueConverter valueConverter = basicType.getValueConverter();

		this.adapted = new ValueConverterTypeAdapter(
				valueConverter.getClass().getTypeName(),
				valueConverter,
				stdIndicators
		);
	}

	@Override
	public BasicType<J> getLegacyResolvedBasicType() {
		return adapted;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return adapted;
	}

	@Override
	public JavaType<J> getDomainJavaType() {
		return basicType.getValueConverter().getDomainJavaType();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return basicType.getValueConverter().getRelationalJavaType();
	}

	@Override
	public JdbcType getJdbcType() {
		return adapted.getJdbcType();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return basicType.getValueConverter();
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return getDomainJavaType().getMutabilityPlan();
	}
}
