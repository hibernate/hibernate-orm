/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.spi.CompositeAttributeFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class NestedCompositeAttributeFetchImpl extends AbstractCompositeFetch implements CompositeAttributeFetch {
	private final FetchSource source;
	private final AttributeDefinition fetchedAttributeDefinition;

	public NestedCompositeAttributeFetchImpl(
			FetchSource source,
			AttributeDefinition fetchedAttributeDefinition,
			ExpandingCompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches) {
		super(
				compositeQuerySpace,
				allowCollectionFetches,
				source.getPropertyPath().append( fetchedAttributeDefinition.getName() )
		);
		this.source = source;
		this.fetchedAttributeDefinition = fetchedAttributeDefinition;
	}

	@Override
	public FetchSource getSource() {
		return source;
	}

	@Override
	public Type getFetchedType() {
		return fetchedAttributeDefinition.getType();
	}

	@Override
	public boolean isNullable() {
		return fetchedAttributeDefinition.isNullable();
	}

	@Override
	public AttributeDefinition getFetchedAttributeDefinition() {
		return fetchedAttributeDefinition;
	}

	@Override
	public EntityReference resolveEntityReference() {
		return source.resolveEntityReference();
	}
}
