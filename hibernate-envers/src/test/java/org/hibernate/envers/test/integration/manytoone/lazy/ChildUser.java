/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import org.hibernate.envers.NotAudited;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.util.List;

/**
 * @author Luke Chen
 */
@Entity
public class ChildUser extends User {

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
	@NotAudited
	private List<Shipment> shipmentList;

	ChildUser() {
	}

	public long getId() {
		return id;
	}
}
