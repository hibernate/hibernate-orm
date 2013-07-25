/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.entity;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * The contract for building {@link UniqueEntityLoader} capable of performing batch-fetch loading.  Intention
 * is to build these instances, by first calling the static {@link #getBuilder}, and then calling the appropriate
 * {@link #buildLoader} method.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.loader.BatchFetchStyle
 */
public abstract class BatchingEntityLoaderBuilder {
	public static BatchingEntityLoaderBuilder getBuilder(SessionFactoryImplementor factory) {
		switch ( factory.getSettings().getBatchFetchStyle() ) {
			case PADDED: {
				return PaddedBatchingEntityLoaderBuilder.INSTANCE;
			}
			case DYNAMIC: {
				return DynamicBatchingEntityLoaderBuilder.INSTANCE;
			}
			default: {
				return org.hibernate.loader.entity.plan.LegacyBatchingEntityLoaderBuilder.INSTANCE;
//				return LegacyBatchingEntityLoaderBuilder.INSTANCE;
			}
		}
	}

	/**
	 * Builds a batch-fetch capable loader based on the given persister, lock-mode, etc.
	 *
	 * @param persister The entity persister
	 * @param batchSize The maximum number of ids to batch-fetch at once
	 * @param lockMode The lock mode
	 * @param factory The SessionFactory
	 * @param influencers Any influencers that should affect the built query
	 *
	 * @return The loader.
	 */
	public UniqueEntityLoader buildLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		if ( batchSize <= 1 ) {
			// no batching
			return buildNonBatchingLoader( persister, lockMode, factory, influencers );
		}
		return buildBatchingLoader( persister, batchSize, lockMode, factory, influencers );
	}

	protected UniqueEntityLoader buildNonBatchingLoader(
			OuterJoinLoadable persister,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return new EntityLoader( persister, lockMode, factory, influencers );
	}

	protected abstract UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers);

	/**
	 * Builds a batch-fetch capable loader based on the given persister, lock-options, etc.
	 *
	 * @param persister The entity persister
	 * @param batchSize The maximum number of ids to batch-fetch at once
	 * @param lockOptions The lock options
	 * @param factory The SessionFactory
	 * @param influencers Any influencers that should affect the built query
	 *
	 * @return The loader.
	 */
	public UniqueEntityLoader buildLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		if ( batchSize <= 1 ) {
			// no batching
			return buildNonBatchingLoader( persister, lockOptions, factory, influencers );
		}
		return buildBatchingLoader( persister, batchSize, lockOptions, factory, influencers );
	}

	protected UniqueEntityLoader buildNonBatchingLoader(
			OuterJoinLoadable persister,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return new EntityLoader( persister, lockOptions, factory, influencers );
	}

	protected abstract UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers);
}
