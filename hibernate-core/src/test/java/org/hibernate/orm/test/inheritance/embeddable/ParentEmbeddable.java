/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import java.io.Serializable;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
//tag::embeddable-inheritance-parent-example[]
@Embeddable
@DiscriminatorValue( "parent" )
@DiscriminatorColumn( name = "embeddable_type" )
class ParentEmbeddable implements Serializable {
	private String parentProp;

	// ...
//end::embeddable-inheritance-parent-example[]

	public ParentEmbeddable() {
	}

	public ParentEmbeddable(String parentProp) {
		this.parentProp = parentProp;
	}

	public String getParentProp() {
		return parentProp;
	}

	public void setParentProp(String parentProp) {
		this.parentProp = parentProp;
	}
//tag::embeddable-inheritance-parent-example[]
}
//end::embeddable-inheritance-parent-example[]
