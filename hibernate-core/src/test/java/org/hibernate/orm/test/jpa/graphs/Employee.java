/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;


/**
 * @author Brett Meyer
 */
@Entity
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
public class Employee {
	@Id @GeneratedValue
	public long id;

	@ManyToMany
	public Set<Manager> managers = new HashSet<Manager>();

	@ManyToMany
	public Set<Employee> friends = new HashSet<Employee>();
}
