/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh17613.a;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.processor.test.hhh17613.ChildB;
import org.hibernate.processor.test.hhh17613.Parent;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

@MappedSuperclass
public abstract class ChildA<A extends Parent, B extends ChildB<A>> extends Parent {

	@OneToMany
	private Set<B> bs = new HashSet<>();

}
