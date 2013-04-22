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
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.visit.LoadPlanVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.visit.LoadPlanVisitor;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.loader.spi.LoadQueryAliasResolutionContext;
import org.hibernate.loader.spi.LoadQueryBuilder;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.spi.WalkingException;

/**
 * @author Gail Badner
 */
public class EntityLoadQueryBuilderImpl implements LoadQueryBuilder {
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LoadPlan loadPlan;
	private final List<JoinableAssociation> associations;

	public EntityLoadQueryBuilderImpl(
			LoadQueryInfluencers loadQueryInfluencers,
			LoadPlan loadPlan) {
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.loadPlan = loadPlan;

	    // TODO: the whole point of the following is to build associations.
		// this could be done while building loadPlan (and be a part of the LoadPlan).
		// Should it be?
		LocalVisitationStrategy strategy = new LocalVisitationStrategy();
		LoadPlanVisitor.visit( loadPlan, strategy );
		this.associations = strategy.associations;
	}

	@Override
	public String generateSql(
			int batchSize,
			SessionFactoryImplementor sessionFactory,
			LoadQueryAliasResolutionContext aliasResolutionContext) {
		return generateSql(
				batchSize,
				getOuterJoinLoadable().getKeyColumnNames(),
				sessionFactory,
				aliasResolutionContext
		);
	}

	public String generateSql(
			int batchSize,
			String[] uniqueKey,
			SessionFactoryImplementor sessionFactory,
			LoadQueryAliasResolutionContext aliasResolutionContext) {
		final EntityLoadQueryImpl loadQuery = new EntityLoadQueryImpl(
				getRootEntityReturn(),
				associations
		);
		return loadQuery.generateSql(
				uniqueKey,
				batchSize,
				getRootEntityReturn().getLockMode(),
				sessionFactory,
				aliasResolutionContext );
	}

	private EntityReturn getRootEntityReturn() {
		return (EntityReturn) loadPlan.getReturns().get( 0 );
	}

	private OuterJoinLoadable getOuterJoinLoadable() {
		return (OuterJoinLoadable) getRootEntityReturn().getEntityPersister();
	}

	private class LocalVisitationStrategy extends LoadPlanVisitationStrategyAdapter {
		private final List<JoinableAssociation> associations = new ArrayList<JoinableAssociation>();
		private Deque<EntityReference> entityReferenceStack = new ArrayDeque<EntityReference>();
		private Deque<CollectionReference> collectionReferenceStack = new ArrayDeque<CollectionReference>();

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
			pushToStack( entityReferenceStack, entityRootReturn );
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
			popFromStack( entityReferenceStack, entityRootReturn );
		}

		@Override
		public void startingEntityFetch(EntityFetch entityFetch) {
			EntityJoinableAssociationImpl assoc = new EntityJoinableAssociationImpl(
					entityFetch,
					getCurrentCollectionReference(),
					"",    // getWithClause( entityFetch.getPropertyPath() )
					false, // hasRestriction( entityFetch.getPropertyPath() )
					loadQueryInfluencers.getEnabledFilters()
			);
			associations.add( assoc );
			pushToStack( entityReferenceStack, entityFetch );
		}

		@Override
		public void finishingEntityFetch(EntityFetch entityFetch) {
			popFromStack( entityReferenceStack, entityFetch );
		}

		@Override
		public void startingCollectionFetch(CollectionFetch collectionFetch) {
			CollectionJoinableAssociationImpl assoc = new CollectionJoinableAssociationImpl(
					collectionFetch,
					getCurrentEntityReference(),
					"",    // getWithClause( entityFetch.getPropertyPath() )
					false, // hasRestriction( entityFetch.getPropertyPath() )
					loadQueryInfluencers.getEnabledFilters()
			);
			associations.add( assoc );
			pushToStack( collectionReferenceStack, collectionFetch );
		}

		@Override
		public void finishingCollectionFetch(CollectionFetch collectionFetch) {
			popFromStack( collectionReferenceStack, collectionFetch );
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
			entityReferenceStack.clear();
			collectionReferenceStack.clear();
		}

		private EntityReference getCurrentEntityReference() {
			return entityReferenceStack.peekFirst() == null ? null : entityReferenceStack.peekFirst();
		}

		private CollectionReference getCurrentCollectionReference() {
			return collectionReferenceStack.peekFirst() == null ? null : collectionReferenceStack.peekFirst();
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
