/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.walking.spi.MetamodelGraphWalker;
import org.hibernate.sql.convert.spi.SqlSelectPlan;

/**
 * A metadata-driven builder of SqmSelectInterpretation.  Coordinates between the
 * {@link MetamodelGraphWalker} and a {@link org.hibernate.persister.common.spi.MetamodelDrivenSqlSelectPlanBuilder}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.persister.walking.spi.MetamodelGraphWalker
 */
public final class MetamodelDrivenSqlSelectPlanBuilder {
	private MetamodelDrivenSqlSelectPlanBuilder() {
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
	public static SqlSelectPlan buildRootEntityLoadPlan(
			org.hibernate.persister.common.spi.MetamodelDrivenSqlSelectPlanBuilder strategy,
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
	public static SqlSelectPlan buildRootCollectionLoadPlan(
			org.hibernate.persister.common.spi.MetamodelDrivenSqlSelectPlanBuilder strategy,
			CollectionPersister persister) {
		MetamodelGraphWalker.visitCollection( strategy, persister );
		return strategy.buildLoadPlan();
	}
}
