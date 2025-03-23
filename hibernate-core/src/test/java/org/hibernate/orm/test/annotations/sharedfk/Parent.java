/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;

import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "PARENT")
public class Parent {
	@Id
	@GeneratedValue
	@Column(name = "ID")
	Integer id;

	@Column(name = "NAME")
	String name;

	@OneToMany( fetch= FetchType.EAGER)
	@JoinColumn(name = "PARENT_ID")
	@OrderColumn(name = "ORDER_C")
	List<ConcreteChild1> child1s = new LinkedList<>();

	@OneToMany( fetch= FetchType.EAGER)
	@JoinColumn(name = "PARENT_ID")
	@OrderColumn(name = "ORDER_C")
	List<ConcreteChild2> child2s= new LinkedList<>();
}
