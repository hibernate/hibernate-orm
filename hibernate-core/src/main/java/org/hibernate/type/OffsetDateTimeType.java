/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetDateTime;
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionType;
import org.hibernate.type.spi.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetDateTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetDateTime>
		implements VersionType<OffsetDateTime>, JdbcLiteralFormatter<OffsetDateTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeType INSTANCE = new OffsetDateTimeType();

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected OffsetDateTimeType() {
		super( TimestampTypeDescriptor.INSTANCE, OffsetDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return OffsetDateTime.class.getSimpleName();
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
		return this;
	}

	@Override
	public String toJdbcLiteral(OffsetDateTime value, Dialect dialect) {
		return toJdbcLiteral( value );
	}

	@SuppressWarnings("WeakerAccess")
	public String toJdbcLiteral(OffsetDateTime value) {
		return DateTimeUtils.formatAsJdbcLiteralTimestamp( value );
	}

}
