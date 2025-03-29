/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 */
@Entity
@Table(name = "entity_composite_fk")
public class EntityWithCompositeIdFkAssociation implements Serializable {

	@Id
	private int id;
	@ManyToOne
	private EntityWithCompositeId association;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public EntityWithCompositeId getAssociation() {
		return association;
	}

	public void setAssociation(EntityWithCompositeId association) {
		this.association = association;
	}
}
