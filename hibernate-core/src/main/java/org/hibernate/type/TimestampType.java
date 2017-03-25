/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.sql.Timestamp}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimestampType
		extends AbstractSingleColumnStandardBasicType<Date>
		implements VersionType<Date>, LiteralType<Date> {

	public static final TimestampType INSTANCE = new TimestampType();

	public TimestampType() {
		super( TimestampTypeDescriptor.INSTANCE, JdbcTimestampTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "timestamp";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Timestamp.class.getName(), java.util.Date.class.getName() };
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
	public Comparator<Date> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

	@Override
	public String objectToSQLString(Date value, Dialect dialect) throws Exception {
		final Timestamp ts = Timestamp.class.isInstance( value )
				? ( Timestamp ) value
				: new Timestamp( value.getTime() );
		// TODO : use JDBC date literal escape syntax? -> {d 'date-string'} in yyyy-mm-dd hh:mm:ss[.f...] format
		return StringType.INSTANCE.objectToSQLString( ts.toString(), dialect );
	}

	@Override
	public Date fromStringValue(String xml) throws HibernateException {
		return fromString( xml );
	}
}
