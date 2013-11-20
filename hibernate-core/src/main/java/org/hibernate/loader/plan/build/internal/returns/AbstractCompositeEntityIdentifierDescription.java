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
