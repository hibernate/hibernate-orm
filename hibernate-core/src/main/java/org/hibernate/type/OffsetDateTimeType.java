/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.descriptor.java.internal.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetDateTimeType
		extends TemporalTypeImpl<OffsetDateTime>
		implements VersionSupport<OffsetDateTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeType INSTANCE = new OffsetDateTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S xxxxx", Locale.ENGLISH );

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected OffsetDateTimeType() {
		super( OffsetDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return OffsetDateTime.class.getSimpleName();
	}

	@Override
	public VersionSupport<OffsetDateTime> getVersionSupport() {
		return this;
	}

	@Override
	public OffsetDateTime seed(SharedSessionContractImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public OffsetDateTime next(OffsetDateTime current, SharedSessionContractImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<OffsetDateTime> getComparator() {
		return OffsetDateTime.timeLineOrder();
	}

	@Override
	public JdbcLiteralFormatter<OffsetDateTime> getJdbcLiteralFormatter() {
		return TimestampTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( OffsetDateTimeJavaDescriptor.INSTANCE );
	}
}
