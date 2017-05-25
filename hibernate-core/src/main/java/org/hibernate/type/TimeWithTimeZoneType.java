/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Time;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimeWithTimeZoneTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIME_WITH_TIMEZONE TIME_WITH_TIMEZONE} and {@link Time}
 *
 * @author Vlad Mihalcea
 */
public class TimeWithTimeZoneType
		extends AbstractSingleColumnStandardBasicType<OffsetTime>
		implements LiteralType<OffsetTime> {

	public static final TimeWithTimeZoneType INSTANCE = new TimeWithTimeZoneType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ss.S xxxxx", Locale.ENGLISH );

	public TimeWithTimeZoneType() {
		super( TimeWithTimeZoneTypeDescriptor.INSTANCE, OffsetTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(OffsetTime value, Dialect dialect) throws Exception {
		return "{t '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public String getName() {
		return "time_with_timezone";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
