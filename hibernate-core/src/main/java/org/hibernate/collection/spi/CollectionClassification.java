/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;

/**
 * Classifications of the plurality.  See {@link CollectionElement.ElementClassification}
 * and {@link CollectionIndex.IndexClassification} for classification of the element and
 * index/key, respectively.
 */
public enum CollectionClassification {
	// todo (6.0) : do we need variants such as SORTED_SET, SORTED_MAP?  ORDERED_*?
	SET( PluralAttribute.CollectionType.SET ),
	LIST( PluralAttribute.CollectionType.LIST ),
	MAP( PluralAttribute.CollectionType.MAP ),
	BAG( PluralAttribute.CollectionType.COLLECTION );

	private final PluralAttribute.CollectionType jpaClassification;

	CollectionClassification(PluralAttribute.CollectionType jpaClassification) {
		this.jpaClassification = jpaClassification;
	}

	public PluralAttribute.CollectionType toJpaClassification() {
		return jpaClassification;
	}
}
