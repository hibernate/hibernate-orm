/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

import org.jboss.logging.Logger;

/**
 * LoadPlanBuildingAssociationVisitationStrategy implementation used for building LoadPlans based on metamodel-defined fetching.  Built
 * LoadPlans contain a single root return object, either an {@link EntityReturn} or a {@link CollectionReturn}.
 *
 * @author Steve Ebersole
 */
public class FetchStyleLoadPlanBuildingAssociationVisitationStrategy
		extends AbstractLoadPlanBuildingAssociationVisitationStrategy {
	private static final Logger log = CoreLogging.logger( FetchStyleLoadPlanBuildingAssociationVisitationStrategy.class );

	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockMode lockMode;

	private Return rootReturn;

	/**
	 * Constructs a FetchStyleLoadPlanBuildingAssociationVisitationStrategy.
	 *
	 * @param sessionFactory The session factory
	 * @param loadQueryInfluencers The options which can influence the SQL query needed to perform the load.
	 * @param lockMode The lock mode.
	 */
	public FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			LockMode lockMode) {
		super( sessionFactory );
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockMode = lockMode;
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

	/**
	 * If required by this strategy, returns a different {@link FetchStrategy} from what is specified
	 * for the given association attribute.
	 *
	 * @param attributeDefinition The association attribute definition.
	 * @param fetchStrategy The fetch strategy for <code>attributeDefinition</code>.
	 * @return the {@link FetchStrategy}, possibly adjusted by this strategy.
	 */
	protected FetchStrategy adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		final Integer maxFetchDepth = sessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
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
		return CollectionReturn.class.isInstance( rootReturn );
	}
}
