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
import org.hibernate.type.descriptor.java.ShortTypeDescriptor;
import org.hibernate.type.descriptor.sql.SmallIntTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#SMALLINT SMALLINT} and {@link Short}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ShortType
		extends AbstractSingleColumnStandardBasicType<Short>
		implements PrimitiveType<Short>, DiscriminatorType<Short>, VersionType<Short> {

	public static final ShortType INSTANCE = new ShortType();

	@SuppressWarnings({ "UnnecessaryBoxing" })
	private static final Short ZERO = Short.valueOf( (short) 0 );

	public ShortType() {
		super( SmallIntTypeDescriptor.INSTANCE, ShortTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "short";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), short.class.getName(), Short.class.getName() };
	}

	public Serializable getDefaultValue() {
		return ZERO;
	}
	
	public Class getPrimitiveClass() {
		return short.class;
	}

	public String objectToSQLString(Short value, Dialect dialect) throws Exception {
		return value.toString();
	}

	public Short stringToObject(String xml) throws Exception {
		return Short.valueOf( xml );
	}

	@SuppressWarnings({ "UnnecessaryBoxing", "UnnecessaryUnboxing" })
	public Short next(Short current, SessionImplementor session) {
		return Short.valueOf( (short) ( current.shortValue() + 1 ) );
	}

	public Short seed(SessionImplementor session) {
		return ZERO;
	}

	public Comparator<Short> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

}
