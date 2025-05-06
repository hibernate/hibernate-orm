/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Gail Badner
 */
@Entity( name="OWNER")
@Table( name="OWNER_TABLE")
public class Owner {
	private Integer id;
	private Set<String> elements = new HashSet<String>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ElementCollection
	public Set<String> getElements() {
		return elements;
	}

	public void setElements(Set<String> elements) {
		this.elements = elements;
	}
}
