/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractCompositeEntityIdentifierDescription
		extends AbstractCompositeFetch
		implements ExpandingEntityIdentifierDescription {

	private final EntityReference entityReference;
	private final CompositeType identifierType;

	protected AbstractCompositeEntityIdentifierDescription(
			EntityReference entityReference,
			ExpandingCompositeQuerySpace compositeQuerySpace,
			CompositeType identifierType,
			PropertyPath propertyPath) {
		super( compositeQuerySpace, false, propertyPath );
		this.entityReference = entityReference;
		this.identifierType = identifierType;
	}

	@Override
	public boolean hasFetches() {
		return getFetches().length > 0;
	}

	@Override
	public boolean hasBidirectionalEntityReferences() {
		return getBidirectionalEntityReferences().length > 0;
	}

	@Override
	public FetchSource getSource() {
		// the source for this (as a Fetch) is the entity reference
		return entityReference;
	}

	@Override
	public Type getFetchedType() {
		return identifierType;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public EntityReference resolveEntityReference() {
		return entityReference;
	}

}
