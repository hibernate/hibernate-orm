/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops.cascade;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;


@Entity
public class B1 {

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	private int id;

	@ManyToOne( fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST} )
	@JoinColumn( name = "aId" )
	private A a;

	@OneToMany( fetch = FetchType.LAZY, mappedBy = "b1", cascade = {CascadeType.PERSIST} )
	private Set<C1> c1List;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public Set<C1> getC1List() {
		if ( c1List == null )
			c1List = new HashSet<C1>();
		return c1List;
	}

	public void setC1List(Set<C1> list) {
		c1List = list;
	}
}
