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
package org.hibernate.loader.plan2.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.EntityReturn;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;

/**
 * @author Steve Ebersole
 */
public class EntityReturnImpl extends AbstractEntityReference implements EntityReturn, ExpandingFetchSource {
	private final EntityQuerySpace entityQuerySpace;

	public EntityReturnImpl(EntityDefinition entityDefinition, LoadPlanBuildingContext loadPlanBuildingContext) {
		super(
				entityDefinition.getEntityPersister(),
				new PropertyPath( entityDefinition.getEntityPersister().getEntityName() )
		);
		this.entityQuerySpace = loadPlanBuildingContext.getQuerySpaces().makeEntityQuerySpace(
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid(),
				entityDefinition.getEntityPersister()
		);
	}

	@Override
	protected EntityQuerySpace getEntityQuerySpace() {
		return entityQuerySpace;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// nothing to do here really
	}
}
