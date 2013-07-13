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

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#DOUBLE DOUBLE} and {@link Double}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DoubleType extends AbstractSingleColumnStandardBasicType<Double> implements PrimitiveType<Double> {
	public static final DoubleType INSTANCE = new DoubleType();

	public static final Double ZERO = 0.0;

	public DoubleType() {
		super( org.hibernate.type.descriptor.sql.DoubleTypeDescriptor.INSTANCE, DoubleTypeDescriptor.INSTANCE );
	}
	@Override
	public String getName() {
		return "double";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), double.class.getName(), Double.class.getName() };
	}
	@Override
	public Serializable getDefaultValue() {
		return ZERO;
	}
	@Override
	public Class getPrimitiveClass() {
		return double.class;
	}
	@Override
	public String objectToSQLString(Double value, Dialect dialect) throws Exception {
		return toString( value );
	}
}
