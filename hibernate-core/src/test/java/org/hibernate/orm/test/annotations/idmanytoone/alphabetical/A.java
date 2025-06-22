/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class A {

	@Id
	private int id;

	@OneToMany( mappedBy = "parent" )
	List<C> children;

	public A() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<C> getChildren() {
		return children;
	}

	public void setChildren(List<C> children) {
		this.children = children;
	}


}
