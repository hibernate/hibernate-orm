/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.jointable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Christian Beikov
 */
@Entity
public class Event extends ABlockableEntity {
	@Column(name = "description")
	private String description;

	public Event() {
	}

	public Event(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
