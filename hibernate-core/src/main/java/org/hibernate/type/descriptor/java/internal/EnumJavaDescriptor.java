/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;
import javax.persistence.EnumType;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaDescriptor<T extends Enum> extends AbstractBasicJavaDescriptor<T> {
	@SuppressWarnings("unchecked")
	protected EnumJavaDescriptor(Class<T> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		final int jdbcCode;
		if ( context.getEnumeratedType() != null && context.getEnumeratedType() == EnumType.STRING ) {
			return context.isNationalized()
					? context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.NVARCHAR )
					: context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
		}
		else {
			return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.INTEGER );
		}
	}

	@Override
	public String toString(T value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	@SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
	public T fromString(String string) {
		return string == null ? null : (T) Enum.valueOf( getJavaType(), string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, WrapperOptions options) {
		return (T) value;
	}
}
