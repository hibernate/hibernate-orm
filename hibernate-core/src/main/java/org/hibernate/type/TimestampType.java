/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionType;
import org.hibernate.type.spi.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.sql.Timestamp}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimestampType
		extends AbstractSingleColumnStandardBasicType<Date>
		implements VersionType<Date>,JdbcLiteralFormatter<Date> {

	public static final TimestampType INSTANCE = new TimestampType();

	public TimestampType() {
		super( TimestampTypeDescriptor.INSTANCE, JdbcTimestampTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "timestamp";
	}

	@Override
	public Date next(Date current, SharedSessionContractImplementor session) {
		return seed( session );
	}

	@Override
	public Date seed(SharedSessionContractImplementor session) {
		return new Timestamp( System.currentTimeMillis() );
	}

	@Override
	public JdbcLiteralFormatter<Date> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Date value, Dialect dialect) {
		return DateTimeUtils.formatAsJdbcLiteralTimestamp( value );
	}
}
