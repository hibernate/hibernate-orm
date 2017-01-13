/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.java.internal.OffsetTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetTimeType extends TemporalTypeImpl<OffsetTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetTimeType INSTANCE = new OffsetTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ss.S xxxxx", Locale.ENGLISH );

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected OffsetTimeType() {
		super( OffsetTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return OffsetTime.class.getSimpleName();
	}

	@Override
	public JdbcLiteralFormatter<OffsetTime> getJdbcLiteralFormatter() {
		return TimeTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( OffsetTimeJavaDescriptor.INSTANCE );
	}

}
