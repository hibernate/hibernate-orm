/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Child")
class Child {
	@Id
	Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	Parent parent;

	Child() {
	}

	Child(Integer id, Parent parent) {
		this.id = id;
		this.parent = parent;
	}
}
