/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.IntegerJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and @link Integer}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class IntegerType extends AbstractSingleColumnStandardBasicType<Integer> {

	public static final IntegerType INSTANCE = new IntegerType();

	public IntegerType() {
		super( IntegerJdbcType.INSTANCE, IntegerJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "integer";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName(), int.class.getName(), Integer.class.getName()};
	}

}
