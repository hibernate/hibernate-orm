/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.transaction.synchronization;

import static org.jboss.logging.Logger.Level.TRACE;
import javax.transaction.Synchronization;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * The {@link Synchronization} implementation Hibernate registers with the JTA {@link javax.transaction.Transaction}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class HibernateSynchronizationImpl implements Synchronization {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                HibernateSynchronizationImpl.class.getPackage().getName());

	private final CallbackCoordinator coordinator;

	public HibernateSynchronizationImpl(CallbackCoordinator coordinator) {
		this.coordinator = coordinator;
	}

	/**
	 * {@inheritDoc}
	 */
	public void beforeCompletion() {
        LOG.jtaSyncBeforeCompletion();
		coordinator.beforeCompletion();
	}

	/**
	 * {@inheritDoc}
	 */
	public void afterCompletion(int status) {
        LOG.jtaSyncAfterCompletion(status);
		coordinator.afterCompletion( status );
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = TRACE )
        @Message( value = "JTA sync : afterCompletion(%d)" )
        void jtaSyncAfterCompletion( int status );

        @LogMessage( level = TRACE )
        @Message( value = "JTA sync : beforeCompletion()" )
        void jtaSyncBeforeCompletion();
    }
}
