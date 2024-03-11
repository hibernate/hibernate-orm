/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Wrapper of {@link CustomMutabilityConvertedBasicTypeImpl} for primitive type.
 *
 * @author Marco Belladelli
 */
public class CustomMutabilityConvertedPrimitiveBasicTypeImpl<J> extends CustomMutabilityConvertedBasicTypeImpl<J> {
	private final Class<J> primitiveClass;

	public CustomMutabilityConvertedPrimitiveBasicTypeImpl(
			String name,
			String description,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter,
			Class<J> primitiveClass,
			MutabilityPlan<J> mutabilityPlan) {
		super( name, description, jdbcType, converter, mutabilityPlan );
		assert primitiveClass.isPrimitive();
		this.primitiveClass = primitiveClass;
	}

	@Override
	public Class<J> getJavaType() {
		return primitiveClass;
	}
}
