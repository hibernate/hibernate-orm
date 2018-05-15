/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.time.ZonedDateTime;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Andrea Boriero
 */
public class ZonedDateTimeVersionSupport implements VersionSupport<ZonedDateTime> {

	public static final ZonedDateTimeVersionSupport INSTANCE = new ZonedDateTimeVersionSupport();

	@Override
	public ZonedDateTime seed(SharedSessionContractImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	public ZonedDateTime next(ZonedDateTime current, SharedSessionContractImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	public String toLoggableString(Object value) {
		return StandardSpiBasicTypes.ZONED_DATE_TIME.toLoggableString( value );
	}

	@Override
	public boolean isEqual(ZonedDateTime x, ZonedDateTime y) throws HibernateException {
		return StandardSpiBasicTypes.ZONED_DATE_TIME.areEqual( x, y );
	}
}
