/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.empty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comp_empty_owner")
public class ComponentEmptyEmbeddedOwner {

	@Id
	@GeneratedValue
	private Integer id;

	private ComponentEmptyEmbedded embedded;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ComponentEmptyEmbedded getEmbedded() {
		return embedded;
	}

	public void setEmbedded(ComponentEmptyEmbedded embedded) {
		this.embedded = embedded;
	}

}
