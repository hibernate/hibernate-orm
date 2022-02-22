/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class ImmutableNamedBasicTypeImpl<J> extends NamedBasicTypeImpl<J> {

	public ImmutableNamedBasicTypeImpl(
			JavaType<J> jtd,
			JdbcType std,
			String name) {
		super( jtd, std, name );
	}

	public ImmutableNamedBasicTypeImpl(
			JavaType<J> javaType,
			JdbcType jdbcType,
			String sqlType,
			Integer lengthOrPrecision,
			Integer scale,
			String name) {
		super( javaType, jdbcType, sqlType, lengthOrPrecision, scale, name );
	}

	@Override
	public BasicType<J> withSqlType(String sqlType, Integer lengthOrPrecision, Integer scale) {
		return lengthOrPrecision == null && scale == null ? this : new ImmutableNamedBasicTypeImpl<>(
				getJavaTypeDescriptor(),
				getJdbcType(),
				sqlType,
				lengthOrPrecision,
				scale,
				getName()
		);
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
