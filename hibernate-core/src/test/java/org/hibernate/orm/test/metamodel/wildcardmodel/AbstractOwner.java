/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.wildcardmodel;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractOwner {

	@Id
	@GeneratedValue
	private Long id;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", targetEntity = AbstractEntity.class)
	private List<? extends AbstractEntity> entities = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<? extends AbstractEntity> getEntities() {
		return entities;
	}

	public void setEntities(List<? extends AbstractEntity> entities) {
		this.entities = entities;
	}

}
