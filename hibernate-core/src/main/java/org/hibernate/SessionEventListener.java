/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * Implemented by custom listeners that respond to low-level events
 * involving interactions between the {@link Session} and the database
 * or second-level cache.
 * <p>
 * A {@code SessionEventListener} class applying to all newly-created
 * sessions may be registered using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#AUTO_SESSION_EVENTS_LISTENER}.
 * A new instance of the class will be created for each new session.
 * <p>
 * <em>This an incubating API, subject to change.</em>
 *
 * @see org.hibernate.cfg.AvailableSettings#AUTO_SESSION_EVENTS_LISTENER
 * @see SessionBuilder#eventListeners(SessionEventListener...)
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SessionEventListener extends Serializable {
	default void transactionCompletion(boolean successful) {}

	default void jdbcConnectionAcquisitionStart() {}
	default void jdbcConnectionAcquisitionEnd() {}

	default void jdbcConnectionReleaseStart() {}
	default void jdbcConnectionReleaseEnd() {}

	default void jdbcPrepareStatementStart() {}
	default void jdbcPrepareStatementEnd() {}

	default void jdbcExecuteStatementStart() {}
	default void jdbcExecuteStatementEnd() {}

	default void jdbcExecuteBatchStart() {}
	default void jdbcExecuteBatchEnd() {}

	default void cachePutStart() {}
	default void cachePutEnd() {}

	default void cacheGetStart() {}
	default void cacheGetEnd(boolean hit) {}

	default void flushStart() {}
	default void flushEnd(int numberOfEntities, int numberOfCollections) {}

	default void prePartialFlushStart(){}
	default void prePartialFlushEnd(){}

	default void partialFlushStart() {}
	default void partialFlushEnd(int numberOfEntities, int numberOfCollections) {}

	default void dirtyCalculationStart() {}
	default void dirtyCalculationEnd(boolean dirty) {}

	default void end() {}
}
