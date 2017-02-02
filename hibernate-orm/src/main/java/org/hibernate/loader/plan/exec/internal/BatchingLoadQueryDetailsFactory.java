/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Queryable;

/**
 * A factory class for creating a {@link org.hibernate.loader.plan.exec.spi.LoadQueryDetails} object.
 */
public class BatchingLoadQueryDetailsFactory {
	/**
	 * Singleton access
	 */
	public static final BatchingLoadQueryDetailsFactory INSTANCE = new BatchingLoadQueryDetailsFactory();

	/**
	 * Private to disallow instantiation
	 */
	private BatchingLoadQueryDetailsFactory() {
	}

	/**
	 * Returns a EntityLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param keyColumnNames The columns to load the entity by (the PK columns or some other unique set of columns)
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 *
	 * @return The EntityLoadQueryDetails
	 */
	public LoadQueryDetails makeEntityLoadQueryDetails(
			LoadPlan loadPlan,
			String[] keyColumnNames,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {

		// TODO: how should shouldUseOptionalEntityInformation be used?
		// final int batchSize = buildingParameters.getBatchSize();
		// final boolean shouldUseOptionalEntityInformation = batchSize == 1;

		final EntityReturn rootReturn = RootHelper.INSTANCE.extractRootReturn( loadPlan, EntityReturn.class );
		final String[] keyColumnNamesToUse = keyColumnNames != null
				? keyColumnNames
				: ( (Queryable) rootReturn.getEntityPersister() ).getIdentifierColumnNames();
		// Should be just one querySpace (of type EntityQuerySpace) in querySpaces.  Should we validate that?
		// Should we make it a util method on Helper like we do for extractRootReturn ?
		final AliasResolutionContextImpl aliasResolutionContext = new AliasResolutionContextImpl( factory );
		return new EntityLoadQueryDetails(
				loadPlan,
				keyColumnNamesToUse,
				aliasResolutionContext,
				rootReturn,
				buildingParameters,
				factory
		);
	}

	/**
	 * Constructs a BasicCollectionLoadQueryDetails object from the given inputs.
	 *
	 * @param collectionPersister The collection persister.
	 * @param loadPlan The load plan.
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 *
	 * @return The EntityLoadQueryDetails
	 */
	public LoadQueryDetails makeCollectionLoadQueryDetails(
			CollectionPersister collectionPersister,
			LoadPlan loadPlan,
			QueryBuildingParameters buildingParameters) {
		final CollectionReturn rootReturn = RootHelper.INSTANCE.extractRootReturn( loadPlan, CollectionReturn.class );
		final AliasResolutionContextImpl aliasResolutionContext = new AliasResolutionContextImpl(
				collectionPersister.getFactory()
		);
		return collectionPersister.isOneToMany() ?
				new OneToManyLoadQueryDetails(
						loadPlan,
						aliasResolutionContext,
						rootReturn,
						buildingParameters,
						collectionPersister.getFactory()
				) :
				new BasicCollectionLoadQueryDetails(
						loadPlan,
						aliasResolutionContext,
						rootReturn,
						buildingParameters,
						collectionPersister.getFactory()
				);
	}

}
