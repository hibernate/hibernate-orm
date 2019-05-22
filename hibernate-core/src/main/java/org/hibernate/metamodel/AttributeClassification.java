/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

public enum AttributeClassification {
	BASIC( PersistentAttributeType.BASIC ),
	EMBEDDED( PersistentAttributeType.EMBEDDED ),
	ANY( null ),
	ONE_TO_ONE( PersistentAttributeType.ONE_TO_ONE ),
	MANY_TO_ONE( PersistentAttributeType.EMBEDDED ),
	ELEMENT_COLLECTION( PersistentAttributeType.ELEMENT_COLLECTION ),
	ONE_TO_MANY( PersistentAttributeType.MANY_TO_ONE ),
	MANY_TO_MANY( PersistentAttributeType.MANY_TO_MANY );

	private final PersistentAttributeType jpaClassification;

	AttributeClassification(PersistentAttributeType jpaClassification) {
		this.jpaClassification = jpaClassification;
	}

	public PersistentAttributeType getJpaClassification() {
		return jpaClassification;
	}
}
