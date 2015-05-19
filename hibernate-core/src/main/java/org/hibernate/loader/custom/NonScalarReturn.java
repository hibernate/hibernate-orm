/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;

/**
 * Represents some non-scalar (entity/collection) return within the query result.
 *
 * @author Steve Ebersole
 */
public abstract class NonScalarReturn implements Return {
	private final String alias;
	private final LockMode lockMode;

	public NonScalarReturn(String alias, LockMode lockMode) {
		this.alias = alias;
		if ( alias == null ) {
			throw new HibernateException("alias must be specified");
		}
		this.lockMode = lockMode;
	}

	public String getAlias() {
		return alias;
	}

	public LockMode getLockMode() {
		return lockMode;
	}
}
