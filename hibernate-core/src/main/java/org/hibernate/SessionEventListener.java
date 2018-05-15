/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * NOTE : Consider this an incubating API, likely to change as wider usage indicates changes that need to be made
 *
 * @author Steve Ebersole
 */
public interface SessionEventListener extends Serializable {
	void transactionCompletion(boolean successful);

	void jdbcConnectionAcquisitionStart();
	void jdbcConnectionAcquisitionEnd();

	void jdbcConnectionReleaseStart();
	void jdbcConnectionReleaseEnd();

	void jdbcPrepareStatementStart();
	void jdbcPrepareStatementEnd();

	void jdbcExecuteStatementStart();
	void jdbcExecuteStatementEnd();

	void jdbcExecuteBatchStart();
	void jdbcExecuteBatchEnd();

	void cachePutStart();
	void cachePutEnd();

	void cacheGetStart();
	void cacheGetEnd(boolean hit);

	void flushStart();
	void flushEnd(int numberOfEntities, int numberOfCollections);

	void partialFlushStart();
	void partialFlushEnd(int numberOfEntities, int numberOfCollections);

	void dirtyCalculationStart();
	void dirtyCalculationEnd(boolean dirty);

	void end();
}
