/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.sql.Types;

import org.hibernate.type.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Character Character[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayType
		extends AbstractSingleColumnStandardBasicType<Character[]>
		implements SqlTypeDescriptorIndicatorCapable<Character[]> {
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
	public <X> BasicType<X> resolveIndicatedType(SqlTypeDescriptorIndicators indicators) {
		if ( indicators.isNationalized() ) {
			final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
			if ( indicators.isLob() ) {
				//noinspection unchecked
				return (BasicType<X>) CharacterArrayNClobType.INSTANCE;
			}
			else {
				final SqlTypeDescriptor nvarcharType = typeConfiguration.getSqlTypeDescriptorRegistry().getDescriptor( Types.NVARCHAR );
				//noinspection unchecked
				return (BasicType<X>) typeConfiguration.getBasicTypeRegistry().resolve( getJavaTypeDescriptor(), nvarcharType );
			}
		}

		if ( indicators.isLob() ) {
			//noinspection unchecked
			return (BasicType<X>) CharacterArrayClobType.INSTANCE;
		}

		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
