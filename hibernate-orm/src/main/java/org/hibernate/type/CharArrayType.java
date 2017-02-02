/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@code char[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharArrayType extends AbstractSingleColumnStandardBasicType<char[]> {
	public static final CharArrayType INSTANCE = new CharArrayType();

	public CharArrayType() {
		super( VarcharTypeDescriptor.INSTANCE, PrimitiveCharacterArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "characters"; 
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "char[]", char[].class.getName() };
	}
}
