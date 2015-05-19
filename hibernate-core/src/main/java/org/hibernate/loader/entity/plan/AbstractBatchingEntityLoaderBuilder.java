/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.entity.BatchingEntityLoaderBuilder;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * Base class for LoadPlan-based BatchingEntityLoaderBuilder implementations.  Mainly we handle the common
 * "no batching" case here to use the LoadPlan-based EntityLoader
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBatchingEntityLoaderBuilder extends BatchingEntityLoaderBuilder {
	@Override
	protected UniqueEntityLoader buildNonBatchingLoader(
			OuterJoinLoadable persister,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return EntityLoader.forEntity( persister ).withLockMode( lockMode ).withInfluencers( influencers ).byPrimaryKey();
	}

	@Override
	protected UniqueEntityLoader buildNonBatchingLoader(
			OuterJoinLoadable persister,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return EntityLoader.forEntity( persister ).withLockOptions( lockOptions ).withInfluencers( influencers ).byPrimaryKey();
	}
}
