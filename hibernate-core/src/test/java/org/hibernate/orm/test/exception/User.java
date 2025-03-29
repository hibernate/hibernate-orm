/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exception;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of User.
 *
 * @author Steve Ebersole
 */
public class User {
	private Long id;
	private String username;
	private Set memberships = new HashSet();

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
