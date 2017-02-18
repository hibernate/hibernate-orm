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
public class BinaryVersionSupport implements VersionSupport<byte[]>  {

	public static final BinaryVersionSupport INSTANCE = new BinaryVersionSupport();

	@Override
	public byte[] seed(SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public byte[] next(byte[] current, SharedSessionContractImplementor session) {
		return current;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return StandardSpiBasicTypes.BINARY.toLoggableString( value,factory );
	}

	@Override
	public boolean isEqual(byte[] x, byte[] y) throws HibernateException {
		return StandardSpiBasicTypes.BINARY.isEqual( x,y );
	}
}
