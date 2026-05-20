/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.partial;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity(name = "PartialOverlayParent")
public class PartialOverlayParent extends PartialOverlayBaseEntity {

	@OneToMany(mappedBy = "parent")
	private List<PartialOverlayChild> children = new ArrayList<>();

	public List<PartialOverlayChild> getChildren() {
		return children;
	}
}
