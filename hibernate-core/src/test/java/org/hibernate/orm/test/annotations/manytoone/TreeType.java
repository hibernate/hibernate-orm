/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class TreeType {
	private Integer id;
	private String name;
	private ForestType forestType;
	private ForestType alternativeForestType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name="Tree_Forest")
	public ForestType getForestType() {
		return forestType;
	}

	public void setForestType(ForestType forestType) {
		this.forestType = forestType;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name="Atl_Forest_Type",
		joinColumns = @JoinColumn(name="tree_id"),
		inverseJoinColumns = @JoinColumn(name="forest_id") )
	public ForestType getAlternativeForestType() {
		return alternativeForestType;
	}

	public void setAlternativeForestType(ForestType alternativeForestType) {
		this.alternativeForestType = alternativeForestType;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
