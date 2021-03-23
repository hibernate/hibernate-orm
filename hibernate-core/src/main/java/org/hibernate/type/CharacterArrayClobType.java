/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Character Character[]}
 * <p/>
 * Essentially a {@link MaterializedClobType} but represented as a Character[] in Java rather than String.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayClobType
		extends AbstractSingleColumnStandardBasicType<Character[]>
		implements SqlTypeDescriptorIndicatorCapable<Character[]> {
	public static final CharacterArrayClobType INSTANCE = new CharacterArrayClobType();

	public CharacterArrayClobType() {
		super( ClobTypeDescriptor.DEFAULT, CharacterArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeDescriptorIndicators indicators) {
		//noinspection unchecked
		return (BasicType<X>) ( indicators.isNationalized() ? CharacterArrayNClobType.INSTANCE : this );
	}
}
