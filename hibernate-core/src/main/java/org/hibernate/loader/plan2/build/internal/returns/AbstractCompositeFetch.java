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
package org.hibernate.loader.plan2.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractCompositeFetch extends AbstractExpandingFetchSource implements CompositeFetch {
	private static final FetchStrategy FETCH_STRATEGY = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private final CompositeType compositeType;
	private final boolean allowCollectionFetches;

	protected AbstractCompositeFetch(
			CompositeType compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches,
			PropertyPath propertyPath) {
		super( compositeQuerySpace, propertyPath );
		this.compositeType = compositeType;
		this.allowCollectionFetches = allowCollectionFetches;
	}

	@Override
	public EntityReference resolveEntityReference() {
		return resolveFetchSourceEntityReference( this );
	}

	private static EntityReference resolveFetchSourceEntityReference(CompositeFetch fetch) {
		final FetchSource fetchSource = fetch.getSource();

		if ( EntityReference.class.isInstance( fetchSource ) ) {
			return (EntityReference) fetchSource;
		}
		else if ( CompositeFetch.class.isInstance( fetchSource ) ) {
			return resolveFetchSourceEntityReference( (CompositeFetch) fetchSource );
		}
		throw new IllegalStateException(
				String.format(
						"Cannot resolve FetchOwner [%s] of Fetch [%s (%s)] to an EntityReference",
						fetchSource,
						fetch,
						fetch.getPropertyPath()
				)
		);
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// anything to do here?
	}

	protected CompositeFetch createCompositeFetch(
			CompositeType compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace) {
		return new NestedCompositeFetchImpl(
				this,
				compositeType,
				compositeQuerySpace,
				allowCollectionFetches,
				getPropertyPath()
		);
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		if ( !allowCollectionFetches ) {
			throw new WalkingException(
					String.format(
							"This composite path [%s] does not allow collection fetches (composite id or composite collection index/element",
							getPropertyPath().getFullPath()
					)
			);
		}
		return super.buildCollectionFetch( attributeDefinition, fetchStrategy, loadPlanBuildingContext );
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

	// this is being removed to be more ogm/search friendly
	@Override
	public String[] toSqlSelectFragments(String alias) {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}
}
