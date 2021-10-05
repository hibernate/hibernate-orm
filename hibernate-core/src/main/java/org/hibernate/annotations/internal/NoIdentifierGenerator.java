/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class NoIdentifierGenerator implements IdentifierGenerator {
	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsJdbcBatchInserts() {
		throw new UnsupportedOperationException();
	}
}
