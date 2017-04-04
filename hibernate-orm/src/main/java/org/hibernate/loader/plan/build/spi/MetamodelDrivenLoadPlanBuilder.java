/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.MetamodelGraphWalker;

/**
 * A metadata-driven builder of LoadPlans.  Coordinates between the {@link MetamodelGraphWalker} and a
 * {@link LoadPlanBuildingAssociationVisitationStrategy}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.persister.walking.spi.MetamodelGraphWalker
 */
public final class MetamodelDrivenLoadPlanBuilder {
	private MetamodelDrivenLoadPlanBuilder() {
	}

	/**
	 * Coordinates building a LoadPlan that defines just a single root entity return (may have fetches).
	 * <p/>
	 * Typically this includes building load plans for entity loading or cascade loading.
	 *
	 * @param strategy The strategy defining the load plan shaping
	 * @param persister The persister for the entity forming the root of the load plan.
	 *
	 * @return The built load plan.
	 */
	public static LoadPlan buildRootEntityLoadPlan(
			LoadPlanBuildingAssociationVisitationStrategy strategy,
			EntityPersister persister) {
		MetamodelGraphWalker.visitEntity( strategy, persister );
		return strategy.buildLoadPlan();
	}

	/**
	 * Coordinates building a LoadPlan that defines just a single root collection return (may have fetches).
	 *
	 * @param strategy The strategy defining the load plan shaping
	 * @param persister The persister for the collection forming the root of the load plan.
	 *
	 * @return The built load plan.
	 */
	public static LoadPlan buildRootCollectionLoadPlan(
			LoadPlanBuildingAssociationVisitationStrategy strategy,
			CollectionPersister persister) {
		MetamodelGraphWalker.visitCollection( strategy, persister );
		return strategy.buildLoadPlan();
	}
}
