/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link String}
 *
 * @author Gavin King
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class MaterializedClobType
		extends AbstractSingleColumnStandardBasicType<String>
		implements AdjustableBasicType<String> {
	public static final MaterializedClobType INSTANCE = new MaterializedClobType();

	public MaterializedClobType() {
		super( ClobTypeDescriptor.DEFAULT, StringTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "materialized_clob";
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		if ( indicators.isNationalized() ) {
			final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
			final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
			final JdbcTypeDescriptor nclobType = jdbcTypeRegistry.getDescriptor( Types.NCLOB );
			return typeConfiguration.getBasicTypeRegistry().resolve( domainJtd, nclobType, getName() );
		}

		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
