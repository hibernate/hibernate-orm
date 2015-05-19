/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ejb;

import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

/**
 * @deprecated Use {@link org.hibernate.jpa.spi.AbstractQueryImpl} instead
 */
@Deprecated
public abstract class AbstractQueryImpl<X> extends org.hibernate.jpa.spi.AbstractQueryImpl<X> {
	protected AbstractQueryImpl(HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
	}
}
