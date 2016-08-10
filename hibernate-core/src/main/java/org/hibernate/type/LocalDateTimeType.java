/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionType;
import org.hibernate.type.spi.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeType
		extends AbstractSingleColumnStandardBasicType<LocalDateTime>
		implements VersionType<LocalDateTime>, JdbcLiteralFormatter<LocalDateTime> {
	/**
	 * Singleton access
	 */
	public static final LocalDateTimeType INSTANCE = new LocalDateTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S", Locale.ENGLISH );

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected LocalDateTimeType() {
		super( TimestampTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalDateTime.class.getSimpleName();
	}

	@Override
	public LocalDateTime seed(SharedSessionContractImplementor session) {
		return LocalDateTime.now();
	}

	@Override
	public LocalDateTime next(LocalDateTime current, SharedSessionContractImplementor session) {
		return LocalDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<LocalDateTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public JdbcLiteralFormatter<LocalDateTime> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(LocalDateTime value, Dialect dialect) {
		return toJdbcLiteral( value );
	}

	@SuppressWarnings("WeakerAccess")
	public String toJdbcLiteral(LocalDateTime value) {
		return DateTimeUtils.formatAsJdbcLiteralTimestamp( value );
	}
}
