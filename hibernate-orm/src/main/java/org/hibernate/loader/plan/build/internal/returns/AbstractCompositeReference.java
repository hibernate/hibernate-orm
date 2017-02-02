/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.CompositeAttributeFetch;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractCompositeReference extends AbstractExpandingFetchSource {

	private final boolean allowCollectionFetches;

	protected AbstractCompositeReference(
			ExpandingCompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches,
			PropertyPath propertyPath) {
		super( compositeQuerySpace, propertyPath );
		this.allowCollectionFetches = allowCollectionFetches;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// anything to do here?
	}

	protected CompositeAttributeFetch createCompositeAttributeFetch(
			AttributeDefinition attributeDefinition,
			ExpandingCompositeQuerySpace compositeQuerySpace) {
		return new NestedCompositeAttributeFetchImpl(
				this,
				attributeDefinition,
				compositeQuerySpace,
				allowCollectionFetches
		);
	}

	@Override
	public CollectionAttributeFetch buildCollectionAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {
		if ( !allowCollectionFetches ) {
			throw new WalkingException(
					String.format(
							"This composite path [%s] does not allow collection fetches (composite id or composite collection index/element",
							getPropertyPath().getFullPath()
					)
			);
		}
		return super.buildCollectionAttributeFetch( attributeDefinition, fetchStrategy );
	}
}
