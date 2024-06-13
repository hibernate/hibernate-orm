/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.annotations.Imported;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
//tag::embeddable-inheritance-sub-child-one-example[]
@Embeddable
@DiscriminatorValue( "sub_child_one" )
class SubChildOneEmbeddable extends ChildOneEmbeddable {
	private Double subChildOneProp;

	// ...
//end::embeddable-inheritance-sub-child-one-example[]

	public SubChildOneEmbeddable() {
	}

	public SubChildOneEmbeddable(String parentProp, Integer childOneProp, Double subChildOneProp) {
		super( parentProp, childOneProp );
		this.subChildOneProp = subChildOneProp;
	}

	public Double getSubChildOneProp() {
		return subChildOneProp;
	}

	public void setSubChildOneProp(Double subChildOneProp) {
		this.subChildOneProp = subChildOneProp;
	}
//tag::embeddable-inheritance-sub-child-one-example[]
}
//end::embeddable-inheritance-sub-child-one-example[]
