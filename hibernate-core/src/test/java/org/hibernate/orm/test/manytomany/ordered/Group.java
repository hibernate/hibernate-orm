/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.ordered;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Transient;

@Entity
@Table(name = "`Group`")
public class Group implements Serializable {

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Transient
	private String org;

	private String name;

	private String description;

	@ManyToMany(mappedBy = "groups")
	@OrderBy("name")
	private List<User> users = new ArrayList<>();

	public Group() {
	}

	public Group(String name, String org) {
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

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addUser(User user) {
		if ( user.getGroups().add( this ) ) {
			getUsers().add( user );
		}
	}
}
