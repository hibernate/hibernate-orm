/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
