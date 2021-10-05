/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.ShortJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#SMALLINT SMALLINT} and {@link Short}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ShortType
		extends AbstractSingleColumnStandardBasicType<Short> {

	public static final ShortType INSTANCE = new ShortType();

	private static final Short ZERO = (short) 0;

	public ShortType() {
		super( SmallIntJdbcTypeDescriptor.INSTANCE, ShortJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "short";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName(), short.class.getName(), Short.class.getName()};
	}

}
