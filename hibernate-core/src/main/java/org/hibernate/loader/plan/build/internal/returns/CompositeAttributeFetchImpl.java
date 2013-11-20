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
public class CompositeAttributeFetchImpl extends AbstractCompositeFetch implements CompositeAttributeFetch {
	private final FetchSource source;
	private final AttributeDefinition fetchedAttribute;

	protected CompositeAttributeFetchImpl(
			FetchSource source,
			AttributeDefinition attributeDefinition,
			ExpandingCompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches) {
		super(
				compositeQuerySpace,
				allowCollectionFetches,
				source.getPropertyPath().append( attributeDefinition.getName() )
		);
		this.source = source;
		this.fetchedAttribute = attributeDefinition;
	}

	@Override
	public FetchSource getSource() {
		return source;
	}

	@Override
	public AttributeDefinition getFetchedAttributeDefinition() {
		return fetchedAttribute;
	}

	@Override
	public Type getFetchedType() {
		return fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return fetchedAttribute.isNullable();
	}

	@Override
	public EntityReference resolveEntityReference() {
		return source.resolveEntityReference();
	}
}
