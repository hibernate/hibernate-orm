/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.ZonedDateTime;
import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ZonedDateTimeComparator;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.spi.descriptor.java.ZonedDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ZonedDateTimeType
		extends TemporalTypeImpl<ZonedDateTime>
		implements VersionSupport<ZonedDateTime> {

	/**
	 * Singleton access
	 */
	public static final ZonedDateTimeType INSTANCE = new ZonedDateTimeType();

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected ZonedDateTimeType() {
		super( ZonedDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return ZonedDateTime.class.getSimpleName();
	}

	@Override
	public VersionSupport<ZonedDateTime> getVersionSupport() {
		return this;
	}

	@Override
	public ZonedDateTime seed(SharedSessionContractImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	public ZonedDateTime next(ZonedDateTime current, SharedSessionContractImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<ZonedDateTime> getComparator() {
		return ZonedDateTimeComparator.INSTANCE;
	}

	@Override
	public JdbcLiteralFormatter<ZonedDateTime> getJdbcLiteralFormatter() {
		return TimestampTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( ZonedDateTimeJavaDescriptor.INSTANCE );
	}
}
