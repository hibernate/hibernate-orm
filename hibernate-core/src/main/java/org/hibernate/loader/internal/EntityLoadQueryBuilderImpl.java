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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.visit.LoadPlanVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.visit.LoadPlanVisitor;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.spi.LoadQueryBuilder;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.spi.WalkingException;

/**
 * @author Gail Badner
 */
public class EntityLoadQueryBuilderImpl implements LoadQueryBuilder {
	private final SessionFactoryImplementor sessionFactory;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LoadPlan loadPlan;
	private final List<JoinableAssociationImpl> associations;

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
	}

	@Override
	public String generateSql(int batchSize) {
		return generateSql( batchSize, getOuterJoinLoadable().getKeyColumnNames() );
	}

	public String generateSql(int batchSize, String[] uniqueKey) {
		final EntityLoadQueryImpl loadQuery = new EntityLoadQueryImpl(
				sessionFactory,
				getRootEntityReturn(),
				associations
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
		private Deque<EntityAliases> entityAliasStack = new ArrayDeque<EntityAliases>();
		private Deque<CollectionAliases> collectionAliasStack = new ArrayDeque<CollectionAliases>();

		private EntityReturn entityRootReturn;

		@Override
		public void handleEntityReturn(EntityReturn rootEntityReturn) {
			this.entityRootReturn = rootEntityReturn;
		}

		@Override
		public void startingRootReturn(Return rootReturn) {
			if ( !EntityReturn.class.isInstance( rootReturn ) ) {
				throw new WalkingException(
						String.format(
								"Unexpected type of return; expected [%s]; instead it was [%s]",
								EntityReturn.class.getName(),
								rootReturn.getClass().getName()
						)
				);
			}
			this.entityRootReturn = (EntityReturn) rootReturn;
			pushToStack( entityAliasStack, entityRootReturn.getEntityAliases() );
		}

		@Override
		public void finishingRootReturn(Return rootReturn) {
			if ( !EntityReturn.class.isInstance( rootReturn ) ) {
				throw new WalkingException(
						String.format(
								"Unexpected type of return; expected [%s]; instead it was [%s]",
								EntityReturn.class.getName(),
								rootReturn.getClass().getName()
						)
				);
			}
			popFromStack( entityAliasStack, ( (EntityReturn) rootReturn ).getEntityAliases() );
		}

		@Override
		public void startingEntityFetch(EntityFetch entityFetch) {
			JoinableAssociationImpl assoc = new JoinableAssociationImpl(
					entityFetch,
					getCurrentCollectionSuffix(),
					"",    // getWithClause( entityFetch.getPropertyPath() )
					false, // hasRestriction( entityFetch.getPropertyPath() )
					sessionFactory,
					loadQueryInfluencers.getEnabledFilters()
			);
			associations.add( assoc );
			pushToStack( entityAliasStack, entityFetch.getEntityAliases() );
		}

		@Override
		public void finishingEntityFetch(EntityFetch entityFetch) {
			popFromStack( entityAliasStack, entityFetch.getEntityAliases() );
		}

		@Override
		public void startingCollectionFetch(CollectionFetch collectionFetch) {
			JoinableAssociationImpl assoc = new JoinableAssociationImpl(
					collectionFetch,
					getCurrentEntitySuffix(),
					"",    // getWithClause( entityFetch.getPropertyPath() )
					false, // hasRestriction( entityFetch.getPropertyPath() )
					sessionFactory,
					loadQueryInfluencers.getEnabledFilters()
			);
			associations.add( assoc );
			pushToStack( collectionAliasStack, collectionFetch.getCollectionAliases() );
		}

		@Override
		public void finishingCollectionFetch(CollectionFetch collectionFetch) {
			popFromStack( collectionAliasStack, collectionFetch.getCollectionAliases() );
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
			entityAliasStack.clear();
			collectionAliasStack.clear();
		}

		private String getCurrentEntitySuffix() {
			return entityAliasStack.peekFirst() == null ? null : entityAliasStack.peekFirst().getSuffix();
		}

		private String getCurrentCollectionSuffix() {
			return collectionAliasStack.peekFirst() == null ? null : collectionAliasStack.peekFirst().getSuffix();
		}

		private <T> void pushToStack(Deque<T> stack, T value) {
			stack.push( value );
		}

		private <T> void popFromStack(Deque<T> stack, T expectedValue) {
			T poppedValue = stack.pop();
			if ( poppedValue != expectedValue ) {
				throw new WalkingException(
						String.format(
								"Unexpected value from stack. Expected=[%s]; instead it was [%s].",
								expectedValue,
								poppedValue
						)
				);
			}
		}
	}
}
