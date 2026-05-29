/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.manytomany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "\"user\"")
public class User implements Serializable {
	@Id
	private Long id;
	@ManyToMany
	@JoinTable(name = "user_group",
			joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_group")),
			inverseJoinColumns = @JoinColumn(name = "group_id", foreignKey = @ForeignKey(name = "fk_group_user")))
	private Set<Group> groups = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set getGroups() {
		return groups;
	}

	public void setGroups(Set groups) {
		this.groups = groups;
	}
}
