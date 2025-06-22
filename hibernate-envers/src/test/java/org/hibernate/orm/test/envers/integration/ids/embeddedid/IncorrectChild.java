/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
public class IncorrectChild {
	@EmbeddedId
	private IncorrectChildId id;

	IncorrectChild() {

	}

	public IncorrectChild(Integer number, Parent parent) {
		this.id = new IncorrectChildId( number, parent );
	}

	public IncorrectChildId getId() {
		return id;
	}

	public void setId(IncorrectChildId id) {
		this.id = id;
	}
}
