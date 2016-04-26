/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.query.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;

/**
 * @author Gail Badner
 */
public class QueryBuildingParametersImpl implements QueryBuildingParameters {
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final int batchSize;
	private final LockMode lockMode;
	private final LockOptions lockOptions;

	public QueryBuildingParametersImpl(
			LoadQueryInfluencers loadQueryInfluencers,
			int batchSize,
			LockMode lockMode,
			LockOptions lockOptions) {
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.batchSize = batchSize;
		this.lockMode = lockMode;
		this.lockOptions = lockOptions;
	}

	@Override
	public LoadQueryInfluencers getQueryInfluencers() {
		return loadQueryInfluencers;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}
}
