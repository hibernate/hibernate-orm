/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ejb;

/**
 * @deprecated Use {@link org.hibernate.jpa.spi.BaseQueryImpl} instead
 */
@Deprecated
public abstract class BaseQueryImpl extends org.hibernate.jpa.spi.BaseQueryImpl {
	public BaseQueryImpl(org.hibernate.jpa.spi.HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
	}
}
