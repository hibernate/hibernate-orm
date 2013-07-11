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

import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeFetch implements CompositeFetch, ExpandingFetchSource {
	private static final FetchStrategy FETCH_STRATEGY = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private final CompositeType compositeType;
	private final PropertyPath propertyPath;

	private List<Fetch> fetches;

	protected AbstractCompositeFetch(CompositeType compositeType, PropertyPath propertyPath) {
		this.compositeType = compositeType;
		this.propertyPath = propertyPath;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// anything to do here?
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		// todo : implement
		return null;
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition, LoadPlanBuildingContext loadPlanBuildingContext) {
		// todo : implement
		return null;
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		// todo : implement
		return null;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return FETCH_STRATEGY;
	}

	@Override
	public Type getFetchedType() {
		return compositeType;
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public String getAdditionalJoinConditions() {
		return null;
	}

	@Override
	public Fetch[] getFetches() {
		return (fetches == null) ? NO_FETCHES : fetches.toArray( new Fetch[fetches.size()] );
	}


	// this is being removed to be more ogm/search friendly
	@Override
	public String[] toSqlSelectFragments(String alias) {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}
}
