/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Andrea Boriero
 */
public class LongVersionSupport implements VersionSupport<Long> {

	public static final LongVersionSupport INSTANCE = new LongVersionSupport();

	private static final Long ZERO = (long) 0;

	@Override
	public Long seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Long next(Long current, SharedSessionContractImplementor session) {
		return current + 1L;
	}

	@Override
	public String toLoggableString(Object value) {
		return StandardSpiBasicTypes.LONG.toLoggableString( value );
	}

	@Override
	public boolean isEqual(Long x, Long y) throws HibernateException {
		return StandardSpiBasicTypes.LONG.areEqual( x, y );
	}
}
