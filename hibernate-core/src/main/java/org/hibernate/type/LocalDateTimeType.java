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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.descriptor.java.internal.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.TimestampSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeType
		extends TemporalTypeImpl<LocalDateTime>
		implements VersionSupport<LocalDateTime> {
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
		super( LocalDateTimeJavaDescriptor.INSTANCE, TimestampSqlDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalDateTime.class.getSimpleName();
	}

	@Override
	public VersionSupport<LocalDateTime> getVersionSupport() {
		return this;
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
		return TimestampSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( LocalDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalType<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision,
			TypeConfiguration typeConfiguration) {
		switch ( precision ) {
			case DATE: {
				return (TemporalType<X>) LocalDateType.INSTANCE;
			}
			case TIME: {
				return (TemporalType<X>) LocalTimeType.INSTANCE;
			}
			default: {
				return (TemporalType<X>) this;
			}
		}
	}
}
