/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link Types#CHAR CHAR(1)} and {@link Character}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CharacterType
		extends AbstractSingleColumnStandardBasicType<Character>
		implements PrimitiveType<Character>, DiscriminatorType<Character>, AdjustableBasicType<Character> {

	public static final CharacterType INSTANCE = new CharacterType();

	public CharacterType() {
		super( CharTypeDescriptor.INSTANCE, CharacterTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "character";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), char.class.getName(), Character.class.getName() };
	}

	public Serializable getDefaultValue() {
		throw new UnsupportedOperationException( "not a valid id type" );
	}

	public Class getPrimitiveClass() {
		return char.class;
	}

	public String objectToSQLString(Character value, Dialect dialect) {
		if ( value == '\'' ) {
			return "''''";
		}
		final char[] chars = new char[3];
		chars[0] = chars[2] = '\'';
		chars[1] = value;
		return new String( chars );
	}

	public Character stringToObject(String xml) {
		return fromString( xml );
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
		final JdbcTypeDescriptor jdbcType = indicators.isNationalized()
				? jdbcTypeRegistry.getDescriptor( Types.NCHAR )
				:  jdbcTypeRegistry.getDescriptor( Types.CHAR );

		return typeConfiguration.getBasicTypeRegistry().resolve( domainJtd, jdbcType, getName() );
	}
}
