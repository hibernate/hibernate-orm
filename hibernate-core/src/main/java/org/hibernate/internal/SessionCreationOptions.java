/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.sql.Connection;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.AfterCompletionAction;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ManagedFlushChecker;

/**
 * @author Steve Ebersole
 */
public interface SessionCreationOptions {
	// todo : (5.2) review this. intended as a consolidation of the options needed to create a Session
	//		comes from building a Session and a EntityManager

	/**
	 * Access to the SessionOwner, which defines the contract for things that can wrap a Session
	 *
	 * @return The SessionOwner
	 *
	 * @deprecated since 6.0 SessionOwner is no longer pertinent due to the
	 * hibernate-entitymanager -> hibernate-core consolidation
	 */
	@Deprecated
	SessionOwner getSessionOwner();

	ExceptionMapper getExceptionMapper();

	AfterCompletionAction getAfterCompletionAction();

	ManagedFlushChecker getManagedFlushChecker();

	Connection getConnection();

	Interceptor getInterceptor();

	StatementInspector getStatementInspector();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	String getTenantIdentifier();

	PersistenceUnitTransactionType getPersistenceUnitTransactionType();

	SynchronizationType getSynchronizationType();

	boolean isClearStateOnCloseEnabled();

	boolean isFlushBeforeCompletionEnabled();

	// not sure of these are needed
//	boolean isAutoCloseSessionEnabled();
//	PersistenceContextType getPersistenceContextType();
}
