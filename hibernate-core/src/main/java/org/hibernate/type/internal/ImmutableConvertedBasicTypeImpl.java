/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class ImmutableConvertedBasicTypeImpl<J> extends ConvertedBasicTypeImpl<J> {

	public ImmutableConvertedBasicTypeImpl(
			JavaType<J> jtd,
			JdbcType std,
			String name,
			BasicValueConverter<J, ?> converter) {
		super( jtd, std, name, converter );
	}

	public ImmutableConvertedBasicTypeImpl(
			JavaType<J> javaType,
			JdbcType jdbcType,
			String sqlType,
			Integer lengthOrPrecision,
			Integer scale, String name,
			BasicValueConverter<J, ?> converter) {
		super( javaType, jdbcType, sqlType, lengthOrPrecision, scale, name, converter);
	}

	@Override
	public BasicType<J> withSqlType(String sqlType, Integer lengthOrPrecision, Integer scale) {
		return lengthOrPrecision == null && scale == null ? this : new ImmutableConvertedBasicTypeImpl<>(
				getJavaTypeDescriptor(),
				getJdbcType(),
				sqlType,
				lengthOrPrecision,
				scale,
				getName(),
				getValueConverter()
		);
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
