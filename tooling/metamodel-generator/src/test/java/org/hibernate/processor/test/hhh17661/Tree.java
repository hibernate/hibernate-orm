/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh17661;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
public abstract class Tree<T extends Tree<T, TR>, TR extends TreeRelation<T>> extends Entity {

	@ManyToOne(fetch = FetchType.LAZY)
	private T parent;

	@OneToMany(mappedBy = "parent")
	private Set<TR> childRelation = new HashSet<>();

	@OneToMany(mappedBy = "child", cascade = {CascadeType.ALL}, orphanRemoval = true)
	private Set<TR> parentRelation = new HashSet<>();
}
