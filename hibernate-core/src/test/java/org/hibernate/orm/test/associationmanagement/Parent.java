/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Parent")
class Parent {
	@Id
	Integer id;

	@OneToMany(mappedBy = "parent")
	Set<Child> children = new HashSet<>();

	Parent() {
	}

	Parent(Integer id) {
		this.id = id;
	}
}
