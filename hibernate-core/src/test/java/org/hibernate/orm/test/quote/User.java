/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`User`", uniqueConstraints = @UniqueConstraint(columnNames = { "house3" }))
public class User implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@ManyToMany
	private Set<Role> roles = new HashSet<Role>();

	// These exist solely for HHH-8464 to ensure that the various forms of quoting are normalized internally
	// (using backticks), including the join column.  Without normalization, the mapping will throw a
	// DuplicateMappingException.
	@ManyToOne
	@JoinColumn(name = "\"house\"")
	private House house;
	@Column(name = "\"house\"", insertable = false, updatable = false )
	private Long house1;
	@Column(name = "`house`", insertable = false, updatable = false )
	private Long house2;

	// test UK on FK w/ global quoting -- see HHH-8638
	// This MUST be initialized.  Several DBs do not allow multiple null values in a unique column.
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "house3")
	private House house3 = new House();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public House getHouse() {
		return house;
	}

	public void setHouse(House house) {
		this.house = house;
	}

	public Long getHouse1() {
		return house1;
	}

	public void setHouse1(Long house1) {
		this.house1 = house1;
	}

	public Long getHouse2() {
		return house2;
	}

	public void setHouse2(Long house2) {
		this.house2 = house2;
	}

	public House getHouse3() {
		return house;
	}

	public void setHouse3(House house3) {
		this.house3 = house3;
	}
}
