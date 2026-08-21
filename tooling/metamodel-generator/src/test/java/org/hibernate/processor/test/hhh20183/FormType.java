/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh20183;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FormType {
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	@JoinColumn(name="entity_type_id", referencedColumnName = "id")
	private EntityType entityType;
	public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
	public EntityType getEntityType() {
		return entityType;
	}
	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}
}
