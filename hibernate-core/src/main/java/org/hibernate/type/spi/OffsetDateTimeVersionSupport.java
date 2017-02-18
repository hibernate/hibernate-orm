/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.time.OffsetDateTime;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class OffsetDateTimeVersionSupport implements VersionSupport<OffsetDateTime> {

	public static final OffsetDateTimeVersionSupport INSTANCE = new OffsetDateTimeVersionSupport();

	@Override
	public OffsetDateTime seed(SharedSessionContractImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public OffsetDateTime next(OffsetDateTime current, SharedSessionContractImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.OFFSET_DATE_TIME.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(OffsetDateTime x, OffsetDateTime y) throws HibernateException {
		return StandardSpiBasicTypes.OFFSET_DATE_TIME.isEqual( x, y );
	}
}
