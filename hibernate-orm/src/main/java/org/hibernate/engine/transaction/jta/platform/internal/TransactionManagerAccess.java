/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import java.io.Serializable;
import javax.transaction.TransactionManager;

/**
 * Provides access to a {@link TransactionManager} for use by {@link TransactionManager}-based
 * {@link JtaSynchronizationStrategy} implementations.
 *
 * @author Steve Ebersole
 */
public interface TransactionManagerAccess extends Serializable {
	/**
	 * Obtain the transaction manager
	 *
	 * @return The transaction manager.
	 */
	public TransactionManager getTransactionManager();
}
