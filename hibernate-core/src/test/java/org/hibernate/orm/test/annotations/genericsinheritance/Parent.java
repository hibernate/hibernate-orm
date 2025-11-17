/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.genericsinheritance;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

@MappedSuperclass
public abstract class Parent<C extends Child> {

	@Id @GeneratedValue Long id;
	@MapKey @OneToMany(mappedBy="parent") Map<Long,C> children = new HashMap<Long,C>();


	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}


	public Map<Long,C> getChildren() {
		return children;
	}
	public void setChildren(Map<Long,C> children) {
		this.children = children;
	}


}
