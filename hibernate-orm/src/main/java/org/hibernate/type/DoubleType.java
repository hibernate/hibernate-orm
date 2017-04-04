/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
