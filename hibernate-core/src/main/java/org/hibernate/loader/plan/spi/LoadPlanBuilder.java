/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.MetadataDrivenModelGraphVisitor;

/**
 * Coordinates building of a {@link LoadPlan} between the {@link org.hibernate.persister.walking.spi.MetadataDrivenModelGraphVisitor} and
 * {@link LoadPlanBuilderStrategy}
 *
 * @author Steve Ebersole
 */
public class LoadPlanBuilder {
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
	public static LoadPlan buildRootEntityLoadPlan(LoadPlanBuilderStrategy strategy, EntityPersister persister) {
		MetadataDrivenModelGraphVisitor.visitEntity( strategy, persister );
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
	public static LoadPlan buildRootCollectionLoadPlan(LoadPlanBuilderStrategy strategy, CollectionPersister persister) {
		MetadataDrivenModelGraphVisitor.visitCollection( strategy, persister );
		return strategy.buildLoadPlan();
	}
}
