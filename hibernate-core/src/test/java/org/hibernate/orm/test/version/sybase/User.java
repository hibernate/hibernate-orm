/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;
import java.util.Set;

/**
 * Implementation of User.
 *
 * @author Steve Ebersole
 */
public class User {
	private Long id;
	private byte[] timestamp;
	private String username;
	private Set groups;
	private Set permissions;

	public User() {
	}

	public User(String username) {
		this.username = username;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public byte[] getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(byte[] timestamp) {
		this.timestamp = timestamp;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Set getGroups() {
		return groups;
	}

	public void setGroups(Set groups) {
		this.groups = groups;
	}

	public Set getPermissions() {
		return permissions;
	}

	public void setPermissions(Set permissions) {
		this.permissions = permissions;
	}
}
