/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.Interceptor;

/**
 * @author Steve Ebersole
 */
public interface Settings {
	public PersistenceUnitTransactionType getTransactionType();

	/**
	 * Should resources held by {@link javax.persistence.EntityManager} instance be released immediately on close?
	 * <p/>
	 * The other option is to release them as part of an after-transaction callback.
	 *
	 * @return {@code true}/{@code false}
	 */
	public boolean isReleaseResourcesOnCloseEnabled();

	public Class<? extends Interceptor> getSessionInterceptorClass();

}
