/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;

import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;
import org.hibernate.type.descriptor.sql.DateTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#DATE DATE} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarDateType extends AbstractSingleColumnStandardBasicType<Calendar> {
	public static final CalendarDateType INSTANCE = new CalendarDateType();

	public CalendarDateType() {
		super( DateTypeDescriptor.INSTANCE, CalendarDateTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "calendar_date";
	}

}
