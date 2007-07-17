package org.hibernate.loader.custom;

import org.hibernate.LockMode;
import org.hibernate.HibernateException;

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
