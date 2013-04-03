/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.LoadPlanVisitor;
import org.hibernate.loader.spi.LoadQueryBuilder;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * @author Gail Badner
 */
public class EntityLoadQueryBuilderImpl implements LoadQueryBuilder {
	private final SessionFactoryImplementor sessionFactory;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LoadPlan loadPlan;
	private final List<JoinableAssociationImpl> associations;
	private final List<String> suffixes;

	public EntityLoadQueryBuilderImpl(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			LoadPlan loadPlan) {
		this.sessionFactory = sessionFactory;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.loadPlan = loadPlan;
		LocalVisitationStrategy strategy = new LocalVisitationStrategy();
		LoadPlanVisitor.visit( loadPlan, strategy );
		this.associations = strategy.associations;
		this.suffixes = strategy.suffixes;
	}

	@Override
	public String generateSql(int batchSize) {
		return generateSql( batchSize, getOuterJoinLoadable().getKeyColumnNames() );
	}

	public String generateSql(int batchSize, String[] uniqueKey) {
		final EntityLoadQueryImpl loadQuery = new EntityLoadQueryImpl(
				sessionFactory,
				getRootEntityReturn(),
				associations,
				suffixes
		);
		return loadQuery.generateSql( uniqueKey, batchSize, getRootEntityReturn().getLockMode() );
	}

	private EntityReturn getRootEntityReturn() {
		return (EntityReturn) loadPlan.getReturns().get( 0 );
	}

	private OuterJoinLoadable getOuterJoinLoadable() {
		return (OuterJoinLoadable) getRootEntityReturn().getEntityPersister();
	}
	private class LocalVisitationStrategy extends LoadPlanVisitationStrategyAdapter {
		private final List<JoinableAssociationImpl> associations = new ArrayList<JoinableAssociationImpl>();
		private final List<String> suffixes = new ArrayList<String>();

		private EntityReturn entityRootReturn;

		@Override
		public void handleEntityReturn(EntityReturn rootEntityReturn) {
			this.entityRootReturn = rootEntityReturn;
		}

		@Override
		public void startingEntityFetch(EntityFetch entityFetch) {
			JoinableAssociationImpl assoc = new JoinableAssociationImpl(
					entityFetch,
					"",    // getWithClause( entityFetch.getPropertyPath() )
					false, // hasRestriction( entityFetch.getPropertyPath() )
					sessionFactory,
					loadQueryInfluencers.getEnabledFilters()
			);
			associations.add( assoc );
			suffixes.add( entityFetch.getEntityAliases().getSuffix() );
		}

		@Override
		public void finishingEntityFetch(EntityFetch entityFetch) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void startingCollectionFetch(CollectionFetch collectionFetch) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void finishingCollectionFetch(CollectionFetch collectionFetch) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void startingCompositeFetch(CompositeFetch fetch) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void finishingCompositeFetch(CompositeFetch fetch) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void finish(LoadPlan loadPlan) {
			//suffixes.add( entityRootReturn.getEntityAliases().getSuffix() );
		}
	}
}
