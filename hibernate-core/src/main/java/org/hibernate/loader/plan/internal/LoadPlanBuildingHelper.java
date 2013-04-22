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
package org.hibernate.loader.plan.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan.spi.AbstractFetchOwner;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;

/**
 * @author Steve Ebersole
 */
public class LoadPlanBuildingHelper {
	public static CollectionFetch buildStandardCollectionFetch(
			FetchOwner fetchOwner,
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return new CollectionFetch(
				loadPlanBuildingContext.getSessionFactory(),
				LockMode.NONE, // todo : for now
				fetchOwner,
				fetchStrategy,
				attributeDefinition.getName()
		);
	}

	public static EntityFetch buildStandardEntityFetch(
			FetchOwner fetchOwner,
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {

		return new EntityFetch(
				loadPlanBuildingContext.getSessionFactory(),
				LockMode.NONE, // todo : for now
				fetchOwner,
				attributeDefinition.getName(),
				fetchStrategy
		);
	}

	public static CompositeFetch buildStandardCompositeFetch(
			FetchOwner fetchOwner,
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return new CompositeFetch(
				loadPlanBuildingContext.getSessionFactory(),
				(AbstractFetchOwner) fetchOwner,
				attributeDefinition.getName()
		);
	}
}
