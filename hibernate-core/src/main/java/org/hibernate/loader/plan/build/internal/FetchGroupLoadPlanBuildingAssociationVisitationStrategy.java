/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal;

import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;

/**
 * @author Gail Badner
 */
public class FetchGroupLoadPlanBuildingAssociationVisitationStrategy extends FetchStyleLoadPlanBuildingAssociationVisitationStrategy {

	private final Set<String> attributesInFetchGroup;
	private final boolean isCacheReadEnabled;

	public FetchGroupLoadPlanBuildingAssociationVisitationStrategy(
			EntityPersister entityPersister,
			String fetchGroupName,
			boolean isCacheReadEnabled,
			SessionFactoryImplementor sessionFactory) {
		super( sessionFactory, LoadQueryInfluencers.NONE, LockMode.NONE);
		if ( isCacheReadEnabled &&
				!entityPersister.getFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() ) {
			throw new IllegalStateException(
					"isCacheReadEnabled cannot be true when the second-level cache is disabled"
			);
		}
		this.isCacheReadEnabled = isCacheReadEnabled;
		final LazyAttributesMetadata lazyAttributesMetadata =
				entityPersister.getEntityMetamodel().getBytecodeEnhancementMetadata().getLazyAttributesMetadata();
		attributesInFetchGroup = lazyAttributesMetadata.getAttributesInFetchGroup( fetchGroupName );
	}

	@Override
	protected FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition) {
		if ( getRootReturn().equals( currentSource() ) ) {
			// This is an association that is in the entity being returned.
			// Associations in the fetch group should to be fetched immediately.
			// Associations not in the fetch group should be delayed.
			if ( attributesInFetchGroup.contains( attributeDefinition.getName() ) ) {
				switch ( attributeDefinition.getAssociationNature() ) {
					case ENTITY: {
						final EntityDefinition entityDefinition = attributeDefinition.toEntityDefinition();
						// If the entity is cached, then FetchStyle should be SELECT to simplify the
						// query, since the entity may be found in the cache.
						return new FetchStrategy(
								FetchTiming.IMMEDIATE,
								isCacheReadEnabled && entityDefinition.getEntityPersister().canReadFromCache() ?
										FetchStyle.SELECT :
										FetchStyle.JOIN
						);
					}
					case COLLECTION: {
						final CollectionDefinition collectionDefinition = attributeDefinition.toCollectionDefinition();
						// If the collection is cached, then FetchStyle should be SELECT to simplify the
						// query, since the collection may be found in the cache.
						return new FetchStrategy(
								FetchTiming.IMMEDIATE,
								isCacheReadEnabled && collectionDefinition.getCollectionPersister().hasCache() ?
										FetchStyle.SELECT :
										FetchStyle.JOIN
						);
					}
					case ANY: {
						// ANY associations can only use FetchStyle.SELECT.
						return new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.SELECT );
					}
					default: {
						throw new AssertionFailure(
								"Unknown association nature: " + attributeDefinition.getAssociationNature()
						);
					}
				}
			}
			else {
				// FetchTiming for all associations in the root entity that are not in the fetch group
				// should be delayed. The association in the entity will either: 1) already be initialized,
				// or 2) in a fetch group that has not been loaded yet (i.e., lazy).
				return new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT );
			}
		}
		else {
			// The association is not in the root; determine FetchStrategy in the usual way.
			return super.determineFetchStrategy( attributeDefinition );
		}
	}
}
