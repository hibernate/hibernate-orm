/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class TimestampVersionSupport implements VersionSupport<Date> {

	public static final TimestampVersionSupport INSTANCE = new TimestampVersionSupport();

	@Override
	public Date seed(SharedSessionContractImplementor session) {
		return new Timestamp( System.currentTimeMillis() );
	}

	@Override
	public Date next(Date current, SharedSessionContractImplementor session) {
		return new Timestamp( System.currentTimeMillis() );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.TIMESTAMP.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(Date x, Date y) throws HibernateException {
		return StandardSpiBasicTypes.TIMESTAMP.isEqual( x, y );
	}
}
