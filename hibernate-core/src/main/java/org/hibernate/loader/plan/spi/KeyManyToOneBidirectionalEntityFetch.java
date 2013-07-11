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
package org.hibernate.loader.plan.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * Represents a key-many-to-one fetch that is bi-directionally join fetched.
 * <p/>
 * For example, consider an Order entity whose primary key is partially made up of the Customer entity to which
 * it is associated.  When we join fetch Customer -> Order(s) and then Order -> Customer we have a bi-directional
 * fetch.  This class would be used to represent the Order -> Customer part of that link.
 *
 * @author Steve Ebersole
 */
public class KeyManyToOneBidirectionalEntityFetch extends EntityFetch implements BidirectionalEntityFetch {
	private final EntityReference targetEntityReference;

	public KeyManyToOneBidirectionalEntityFetch(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			FetchOwner owner,
			AttributeDefinition fetchedAttribute,
			EntityReference targetEntityReference,
			FetchStrategy fetchStrategy) {
		super( sessionFactory, lockMode, owner, fetchedAttribute, fetchStrategy );
		this.targetEntityReference = targetEntityReference;
	}

	public KeyManyToOneBidirectionalEntityFetch(
			KeyManyToOneBidirectionalEntityFetch original,
			CopyContext copyContext,
			FetchOwner fetchOwnerCopy) {
		super( original, copyContext, fetchOwnerCopy );
		this.targetEntityReference = original.targetEntityReference;
	}

	public EntityReference getTargetEntityReference() {
		return targetEntityReference;
	}
}
