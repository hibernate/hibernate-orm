/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionType;
import org.hibernate.type.spi.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class InstantType
		extends AbstractSingleColumnStandardBasicType<Instant>
		implements VersionType<Instant>,JdbcLiteralFormatter<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantType INSTANCE = new InstantType();

	protected InstantType() {
		super( TimestampTypeDescriptor.INSTANCE, InstantJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return Instant.class.getSimpleName();
	}

	@Override
	public Instant seed(SharedSessionContractImplementor session) {
		return Instant.now();
	}

	@Override
	public Instant next(Instant current, SharedSessionContractImplementor session) {
		return Instant.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<Instant> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public JdbcLiteralFormatter<Instant> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Instant value, Dialect dialect) {
		return DateTimeUtils.formatAsJdbcLiteralTimestamp( ZonedDateTime.ofInstant( value, ZoneId.of( "UTC" ) ) );
	}
}
