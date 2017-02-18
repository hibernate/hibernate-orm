/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.time.Instant;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Andrea Boriero
 */
public class InstantVersionSupport implements VersionSupport<Instant> {

	public static final InstantVersionSupport INSTANCE = new InstantVersionSupport();

	@Override
	public Instant seed(SharedSessionContractImplementor session) {
		return Instant.now();
	}

	@Override
	public Instant next(Instant current, SharedSessionContractImplementor session) {
		return Instant.now();
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.INSTANT.toLoggableString( value, factory );
	}

	@Override
	public boolean isEqual(Instant x, Instant y) throws HibernateException {
		return StandardSpiBasicTypes.INSTANT.isEqual( x, y );
	}
}
