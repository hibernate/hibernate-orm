/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.LocalTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalTimeType extends AbstractSingleColumnStandardBasicType<LocalTime> implements JdbcLiteralFormatter<LocalTime> {
	/**
	 * Singleton access
	 */
	public static final LocalTimeType INSTANCE = new LocalTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ss", Locale.ENGLISH );

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected LocalTimeType() {
		super( TimeTypeDescriptor.INSTANCE, LocalTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalTime.class.getSimpleName();
	}

	@Override
	public JdbcLiteralFormatter<LocalTime> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(LocalTime value, Dialect dialect) {
		return toJdbcLiteral( value );
	}

	@SuppressWarnings("WeakerAccess")
	public String toJdbcLiteral(LocalTime value) {
		return DateTimeUtils.formatAsJdbcLiteralTime( value );
	}
}
