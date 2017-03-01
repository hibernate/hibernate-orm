/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class IntegerVersionSupport implements VersionSupport<Integer> {

	public static final IntegerVersionSupport INSTANCE = new IntegerVersionSupport();

	public static final Integer ZERO = 0;

	@Override
	public Integer seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Integer next(Integer current, SharedSessionContractImplementor session) {
		return current + 1;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.INTEGER.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(Integer x, Integer y) throws HibernateException {
		return StandardSpiBasicTypes.INTEGER.isEqual( x, y );
	}
}
