/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Types;

import org.hibernate.type.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.NClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link Types#NCLOB NCLOB} and {@link Character Character[]}
 * <p/>
 * Essentially a {@link MaterializedNClobType} but represented as a Character[] in Java rather than String.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayNClobType
		extends AbstractSingleColumnStandardBasicType<Character[]>
		implements AdjustableBasicType<Character[]> {
	public static final CharacterArrayNClobType INSTANCE = new CharacterArrayNClobType();

	public CharacterArrayNClobType() {
		super( NClobTypeDescriptor.DEFAULT, CharacterArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		if ( domainJtd != null && domainJtd.getJavaTypeClass() == char[].class ) {
			// domainJtd is a `char[]` instead of a `Character[]`....
			final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
			final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
			final JdbcTypeDescriptor jdbcType = jdbcTypeRegistry.getDescriptor( Types.NCLOB );

			return typeConfiguration.getBasicTypeRegistry().resolve(
					typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( domainJtd.getJavaType() ),
					jdbcType,
					getName()
			);
		}

		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
