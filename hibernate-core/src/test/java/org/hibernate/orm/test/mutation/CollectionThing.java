/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mutation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

@Entity(name = "CollectionThing")
@Table(name = "stored_collection_thing")
class CollectionThing {
	@Id
	Long id;

	@ElementCollection
	@BatchSize(size = 10)
	@CollectionTable(name = "stored_collection_labels")
	Set<String> labels = new HashSet<>();

	CollectionThing() {
	}

	CollectionThing(Long id) {
		this.id = id;
	}
}
