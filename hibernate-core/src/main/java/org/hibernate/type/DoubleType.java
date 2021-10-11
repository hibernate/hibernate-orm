/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.DoubleJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DoubleJdbcType;

/**
 * A type that maps between {@link java.sql.Types#DOUBLE DOUBLE} and {@link Double}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DoubleType
		extends AbstractSingleColumnStandardBasicType<Double> {
	public static final DoubleType INSTANCE = new DoubleType();

	public DoubleType() {
		super( DoubleJdbcType.INSTANCE, DoubleJavaTypeDescriptor.INSTANCE );
	}
	@Override
	public String getName() {
		return "double";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), double.class.getName(), Double.class.getName() };
	}
}
