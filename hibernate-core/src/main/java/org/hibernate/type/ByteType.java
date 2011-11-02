/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;
import org.hibernate.type.descriptor.sql.TinyIntTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TINYINT TINYINT} and {@link Byte}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings({ "UnnecessaryBoxing" })
public class ByteType
		extends AbstractSingleColumnStandardBasicType<Byte>
		implements PrimitiveType<Byte>, DiscriminatorType<Byte>, VersionType<Byte> {

	public static final ByteType INSTANCE = new ByteType();

	private static final Byte ZERO = Byte.valueOf( (byte)0 );

	public ByteType() {
		super( TinyIntTypeDescriptor.INSTANCE, ByteTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "byte";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), byte.class.getName(), Byte.class.getName() };
	}

	public Serializable getDefaultValue() {
		return ZERO;
	}

	public Class getPrimitiveClass() {
		return byte.class;
	}

	public String objectToSQLString(Byte value, Dialect dialect) {
		return toString( value );
	}

	public Byte stringToObject(String xml) {
		return fromString( xml );
	}

	public Byte fromStringValue(String xml) {
		return fromString( xml );
	}

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	public Byte next(Byte current, SessionImplementor session) {
		return Byte.valueOf( (byte) ( current.byteValue() + 1 ) );
	}

	public Byte seed(SessionImplementor session) {
		return ZERO;
	}

	public Comparator<Byte> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
}
