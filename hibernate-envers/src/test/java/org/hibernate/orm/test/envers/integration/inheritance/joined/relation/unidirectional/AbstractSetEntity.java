/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.relation.unidirectional;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "AbstrSet")
@Inheritance(strategy = InheritanceType.JOINED)
@Audited
public abstract class AbstractSetEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany
	private Set<AbstractContainedEntity> entities = new HashSet<AbstractContainedEntity>();

	public AbstractSetEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<AbstractContainedEntity> getEntities() {
		return entities;
	}

	public void setEntities(Set<AbstractContainedEntity> entities) {
		this.entities = entities;
	}
}
