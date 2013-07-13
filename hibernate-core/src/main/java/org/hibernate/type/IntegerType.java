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
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and @link Integer}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class IntegerType extends AbstractSingleColumnStandardBasicType<Integer>
		implements PrimitiveType<Integer>, DiscriminatorType<Integer>, VersionType<Integer> {

	public static final IntegerType INSTANCE = new IntegerType();

	public static final Integer ZERO = 0;

	public IntegerType() {
		super( org.hibernate.type.descriptor.sql.IntegerTypeDescriptor.INSTANCE, IntegerTypeDescriptor.INSTANCE );
	}
	@Override
	public String getName() {
		return "integer";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), int.class.getName(), Integer.class.getName() };
	}
	@Override
	public Serializable getDefaultValue() {
		return ZERO;
	}
	@Override
	public Class getPrimitiveClass() {
		return int.class;
	}
	@Override
	public String objectToSQLString(Integer value, Dialect dialect) throws Exception {
		return toString( value );
	}
	@Override
	public Integer stringToObject(String xml) {
		return fromString( xml );
	}
	@Override
	public Integer seed(SessionImplementor session) {
		return ZERO;
	}
	@Override
	public Integer next(Integer current, SessionImplementor session) {
		return current+1;
	}
	@Override
	public Comparator<Integer> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
}
