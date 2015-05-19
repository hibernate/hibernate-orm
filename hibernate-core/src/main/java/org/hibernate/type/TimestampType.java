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
import org.hibernate.engine.spi.SessionImplementor;
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

	public String getName() {
		return "timestamp";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Timestamp.class.getName(), java.util.Date.class.getName() };
	}

	public Date next(Date current, SessionImplementor session) {
		return seed( session );
	}

	public Date seed(SessionImplementor session) {
		return new Timestamp( System.currentTimeMillis() );
	}

	public Comparator<Date> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

	public String objectToSQLString(Date value, Dialect dialect) throws Exception {
		final Timestamp ts = Timestamp.class.isInstance( value )
				? ( Timestamp ) value
				: new Timestamp( value.getTime() );
		// TODO : use JDBC date literal escape syntax? -> {d 'date-string'} in yyyy-mm-dd hh:mm:ss[.f...] format
		return StringType.INSTANCE.objectToSQLString( ts.toString(), dialect );
	}

	public Date fromStringValue(String xml) throws HibernateException {
		return fromString( xml );
	}
}
