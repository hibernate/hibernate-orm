/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exception;

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

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "T_USER")
public class User {
	@Id
	@GeneratedValue
	@Column(name = "user_id")
	private Long id;
	@Column(name = "user_name", nullable = false)
	private String username;
	@ManyToMany
	@JoinTable(name = "T_MEMBERSHIP",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "group_id"))
	private Set<Group> memberships = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Set getMemberships() {
		return memberships;
	}

	public void setMemberships(Set memberships) {
		this.memberships = memberships;
	}

	public void addMembership(Group membership) {
		if (membership == null) {
			throw new IllegalArgumentException("Membership to add cannot be null");
		}

		this.memberships.add(membership);
		membership.getMembers().add(this);
	}
}
