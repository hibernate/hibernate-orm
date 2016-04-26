/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.AfterCompletionAction;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class AfterCompletionActionLegacyJpaImpl implements AfterCompletionAction {
	private static final Logger log = Logger.getLogger( AfterCompletionActionLegacyJpaImpl.class );

	/**
	 * Singleton access
	 */
	public static final AfterCompletionActionLegacyJpaImpl INSTANCE = new AfterCompletionActionLegacyJpaImpl();

	@Override
	public void doAction(boolean successful, SessionImplementor session) {
		if ( session.isClosed() ) {
			log.trace( "Session was closed; nothing to do" );
			return;
		}

		if ( !successful && session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta() ) {
			session.clear();
		}
	}
}
