/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.resource.transaction.TransactionCoordinator;

/**
 * An extension of SessionCreationOptions for cases where the Session to be created shares
 * some part of the "transaction context" of another Session.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.SharedSessionBuilder
 */
public interface SharedSessionCreationOptions extends SessionCreationOptions {
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinatorImpl getJdbcCoordinator();
	Transaction getTransaction();
	ActionQueue.TransactionCompletionProcesses getTransactionCompletionProcesses();
}
