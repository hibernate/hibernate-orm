/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.descriptor.sql.NCharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#NCHAR NCHAR(1)} and {@link Character}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CharacterNCharType
		extends AbstractSingleColumnStandardBasicType<Character>
		implements PrimitiveType<Character>, DiscriminatorType<Character> {

	public static final CharacterNCharType INSTANCE = new CharacterNCharType();

	public CharacterNCharType() {
		super( NCharTypeDescriptor.INSTANCE, CharacterTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "ncharacter";
	}

	public Serializable getDefaultValue() {
		throw new UnsupportedOperationException( "not a valid id type" );
	}

	public Class getPrimitiveClass() {
		return char.class;
	}

	public String objectToSQLString(Character value, Dialect dialect) {
		return '\'' + toString( value ) + '\'';
	}

	public Character stringToObject(String xml) {
		return fromString( xml );
	}

}
