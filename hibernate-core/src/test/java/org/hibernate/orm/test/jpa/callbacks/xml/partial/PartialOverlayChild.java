/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.partial;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "PartialOverlayChild")
public class PartialOverlayChild extends PartialOverlayBaseEntity {

	@ManyToOne(optional = false)
	@JoinColumn(name = "parent_id", nullable = false)
	private PartialOverlayParent parent;

	public PartialOverlayParent getParent() {
		return parent;
	}

	public void setParent(PartialOverlayParent parent) {
		this.parent = parent;
	}
}
