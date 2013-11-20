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
import org.hibernate.loader.plan.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * Represents an entity fetch that is bi-directionally join fetched.
 * <p/>
 * For example, consider an Order entity whose primary key is partially made up of the Customer entity to which
 * it is associated.  When we join fetch Customer -> Order(s) and then Order -> Customer we have a bi-directional
 * fetch.  This class would be used to represent the Order -> Customer part of that link.
 *
 * @author Steve Ebersole
 */
public class BidirectionalEntityReferenceImpl implements BidirectionalEntityReference {
	private final EntityReference targetEntityReference;
	private final PropertyPath propertyPath;

	public BidirectionalEntityReferenceImpl(
			ExpandingFetchSource fetchSource,
			AssociationAttributeDefinition fetchedAttribute,
			EntityReference targetEntityReference) {
		this.targetEntityReference = targetEntityReference;
		this.propertyPath = fetchSource.getPropertyPath().append( fetchedAttribute.getName() );
	}

	public EntityReference getTargetEntityReference() {
		return targetEntityReference;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public String getQuerySpaceUid() {
		return targetEntityReference.getQuerySpaceUid();
	}

	@Override
	public Fetch[] getFetches() {
		return targetEntityReference.getFetches();
	}

	@Override
	public BidirectionalEntityReference[] getBidirectionalEntityReferences() {
		return targetEntityReference.getBidirectionalEntityReferences();
	}

	@Override
	public EntityReference resolveEntityReference() {
		return this;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return targetEntityReference.getEntityPersister();
	}

	@Override
	public EntityIdentifierDescription getIdentifierDescription() {
		return targetEntityReference.getIdentifierDescription();
	}
}
