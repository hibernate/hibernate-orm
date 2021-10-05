/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Date;

import org.hibernate.type.descriptor.java.JdbcDateJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DateJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#DATE DATE} and {@link java.sql.Date}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DateType
		extends AbstractSingleColumnStandardBasicType<Date> {

	public static final DateType INSTANCE = new DateType();

	public DateType() {
		super( DateJdbcTypeDescriptor.INSTANCE, JdbcDateJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "date";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				getName(),
				java.sql.Date.class.getName()
		};
	}

}
