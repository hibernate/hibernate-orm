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
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link Types#VARCHAR VARCHAR} and {@link Character Character[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayType
		extends AbstractSingleColumnStandardBasicType<Character[]>
		implements AdjustableBasicType<Character[]> {
	public static final CharacterArrayType INSTANCE = new CharacterArrayType();

	public CharacterArrayType() {
		super( VarcharTypeDescriptor.INSTANCE, CharacterArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "wrapper-characters";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Character[].class.getName(), "Character[]" };
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();

		final int jdbcTypeCode;
		if ( indicators.isLob() ) {
			jdbcTypeCode = indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else {
			jdbcTypeCode = indicators.isNationalized() ? Types.NVARCHAR : Types.VARCHAR;
		}

		final JdbcTypeDescriptor indicatedJdbcType = jdbcTypeRegistry.getDescriptor( jdbcTypeCode );

		if ( domainJtd != null && domainJtd.getJavaTypeClass() == Character[].class ) {
			return typeConfiguration.getBasicTypeRegistry().resolve(
					typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( domainJtd.getJavaType() ),
					indicatedJdbcType,
					getName()
			);
		}

		if ( getJdbcTypeDescriptor() == indicatedJdbcType ) {
			return (BasicType<X>) this;
		}

		return (BasicType<X>) typeConfiguration.getBasicTypeRegistry().resolve(
				getJavaTypeDescriptor(),
				indicatedJdbcType,
				getName()
		);
	}
}
