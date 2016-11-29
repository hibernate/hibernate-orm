/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetTime;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetTimeType extends BasicTypeImpl<OffsetTime> implements JdbcLiteralFormatter<OffsetTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetTimeType INSTANCE = new OffsetTimeType();

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
		return this;
	}

	@Override
	public String toJdbcLiteral(OffsetTime value, Dialect dialect) {
		return toJdbcLiteral( value );
	}

	@SuppressWarnings("WeakerAccess")
	public String toJdbcLiteral(OffsetTime value) {
		return DateTimeUtils.formatAsJdbcLiteralTime( value );
	}
}
