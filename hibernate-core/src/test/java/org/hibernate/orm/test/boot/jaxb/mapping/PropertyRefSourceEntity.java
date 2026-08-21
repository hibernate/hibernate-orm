/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "property_ref_source")
public class PropertyRefSourceEntity {
	@Id
	private Integer id;
	@ManyToOne
	private PropertyRefTargetEntity target;

	private PropertyRefSourceEntity() {
	}

	public PropertyRefSourceEntity(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public PropertyRefTargetEntity getTarget() {
		return target;
	}

	public void setTarget(PropertyRefTargetEntity target) {
		this.target = target;
	}
}
