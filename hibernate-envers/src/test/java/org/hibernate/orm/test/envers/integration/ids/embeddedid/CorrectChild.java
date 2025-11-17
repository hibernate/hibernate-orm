/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
public class CorrectChild {
	@EmbeddedId
	private CorrectChildId id;

	@ManyToOne
	@MapsId("id")
	private Parent parent;

	CorrectChild() {

	}

	public CorrectChild(Integer number, Parent parent) {
		this.id = new CorrectChildId(number, parent);
		this.parent = parent;
	}

	public CorrectChildId getId() {
		return id;
	}

	public void setId(CorrectChildId id) {
		this.id = id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
