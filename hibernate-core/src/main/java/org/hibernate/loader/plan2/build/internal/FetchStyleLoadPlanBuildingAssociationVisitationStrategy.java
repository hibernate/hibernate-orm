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
package org.hibernate.loader.plan2.build.internal;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan2.build.spi.AbstractLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan2.spi.CollectionReturn;
import org.hibernate.loader.plan2.spi.EntityReturn;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.loader.plan2.spi.Return;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * LoadPlanBuilderStrategy implementation used for building LoadPlans based on metamodel-defined fetching.  Built
 * LoadPlans contain a single root return object, either an {@link EntityReturn} or a {@link CollectionReturn}.
 *
 * @author Steve Ebersole
 */
public class FetchStyleLoadPlanBuildingAssociationVisitationStrategy
		extends AbstractLoadPlanBuildingAssociationVisitationStrategy {
	private static final Logger log = CoreLogging.logger( FetchStyleLoadPlanBuildingAssociationVisitationStrategy.class );

	private final LoadQueryInfluencers loadQueryInfluencers;

	private Return rootReturn;

	public FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( sessionFactory );
		this.loadQueryInfluencers = loadQueryInfluencers;
	}

	@Override
	protected boolean supportsRootEntityReturns() {
		return true;
	}

	@Override
	protected boolean supportsRootCollectionReturns() {
		return true;
	}

	@Override
	protected void addRootReturn(Return rootReturn) {
		if ( this.rootReturn != null ) {
			throw new HibernateException( "Root return already identified" );
		}
		this.rootReturn = rootReturn;
	}

	@Override
	public LoadPlan buildLoadPlan() {
		log.debug( "Building LoadPlan..." );

		if ( EntityReturn.class.isInstance( rootReturn ) ) {
			return new LoadPlanImpl( (EntityReturn) rootReturn, getQuerySpaces() );
		}
		else if ( CollectionReturn.class.isInstance( rootReturn ) ) {
			return new LoadPlanImpl( (CollectionReturn) rootReturn, getQuerySpaces() );
		}
		else {
			throw new IllegalStateException( "Unexpected root Return type : " + rootReturn );
		}
	}

	@Override
	protected FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition) {
		FetchStrategy fetchStrategy = attributeDefinition.determineFetchPlan( loadQueryInfluencers, currentPropertyPath );
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchStrategy = adjustJoinFetchIfNeeded( attributeDefinition, fetchStrategy );
		}
		return fetchStrategy;
	}

	protected FetchStrategy adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {
		final Integer maxFetchDepth = sessionFactory().getSettings().getMaximumFetchDepth();
		if ( maxFetchDepth != null && currentDepth() > maxFetchDepth ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		if ( attributeDefinition.getType().isCollectionType() && isTooManyCollections() ) {
			// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		return fetchStrategy;
	}

	@Override
	protected boolean isTooManyCollections() {
		return false;
	}

//	@Override
//	protected EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition) {
//		final String entityName = entityDefinition.getEntityPersister().getEntityName();
//		return new EntityReturn(
//				sessionFactory(),
//				LockMode.NONE, // todo : for now
//				entityName
//		);
//	}
//
//	@Override
//	protected CollectionReturn buildRootCollectionReturn(CollectionDefinition collectionDefinition) {
//		final CollectionPersister persister = collectionDefinition.getCollectionPersister();
//		final String collectionRole = persister.getRole();
//		return new CollectionReturn(
//				sessionFactory(),
//				LockMode.NONE, // todo : for now
//				persister.getOwnerEntityPersister().getEntityName(),
//				StringHelper.unqualify( collectionRole )
//		);
//	}
}
