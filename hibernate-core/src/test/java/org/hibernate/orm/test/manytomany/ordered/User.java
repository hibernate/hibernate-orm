/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.ordered;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "`User`")
public class User implements Serializable {

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "ORG")
	private String org;

	@Column(name = "NAME")
	private String name;

	@ManyToMany
	@JoinTable(
			name = "UserGroup",
			joinColumns = @JoinColumn(name = "USER_ID"),
			inverseJoinColumns = @JoinColumn(name = "GROUP_ID")
	)
	private Set<Group> groups = new HashSet<>();

	public User() {
	}

	public User(String name, String org) {
		this.org = org;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}

}
