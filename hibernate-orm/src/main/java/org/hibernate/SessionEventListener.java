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
	public void transactionCompletion(boolean successful);

	public void jdbcConnectionAcquisitionStart();
	public void jdbcConnectionAcquisitionEnd();

	public void jdbcConnectionReleaseStart();
	public void jdbcConnectionReleaseEnd();

	public void jdbcPrepareStatementStart();
	public void jdbcPrepareStatementEnd();

	public void jdbcExecuteStatementStart();
	public void jdbcExecuteStatementEnd();

	public void jdbcExecuteBatchStart();
	public void jdbcExecuteBatchEnd();

	public void cachePutStart();
	public void cachePutEnd();

	public void cacheGetStart();
	public void cacheGetEnd(boolean hit);

	public void flushStart();
	public void flushEnd(int numberOfEntities, int numberOfCollections);

	public void partialFlushStart();
	public void partialFlushEnd(int numberOfEntities, int numberOfCollections);

	public void dirtyCalculationStart();
	public void dirtyCalculationEnd(boolean dirty);

	public void end();
}
