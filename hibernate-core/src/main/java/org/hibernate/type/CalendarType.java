/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hibernate.type.descriptor.java.CalendarJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarType
		extends AbstractSingleColumnStandardBasicType<Calendar> {

	public static final CalendarType INSTANCE = new CalendarType();

	public CalendarType() {
		super( TimestampJdbcTypeDescriptor.INSTANCE, CalendarJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "calendar";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Calendar.class.getName(), GregorianCalendar.class.getName() };
	}

}
