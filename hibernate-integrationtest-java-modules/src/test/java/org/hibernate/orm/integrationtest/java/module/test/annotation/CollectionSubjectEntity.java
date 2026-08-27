/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import java.util.Collection;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "CollectionSubjectEntity")
public class CollectionSubjectEntity {
	@Id
	public Long id;

	@ElementCollection
	public Collection<String> elements;
}
