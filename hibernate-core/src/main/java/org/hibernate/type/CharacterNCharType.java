/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
