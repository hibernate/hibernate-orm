/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.db;

import java.sql.Timestamp;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SourceType;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "db_vers_user")
public class User {
	@Id
	@Column(name = "user_id")
	private Integer id;

	@Version
	@CurrentTimestamp(source = SourceType.DB)
	@Column(name = "ts")
	private Timestamp timestamp;

	@Column(name = "user_name", unique = true, nullable = false)
	private String username;

	@ManyToMany(mappedBy = "users")
	@BatchSize(size = 9)
	@Fetch(FetchMode.JOIN)
	@OptimisticLock(excluded = false)
	private Set<Group> groups;

	@ManyToMany
	@JoinTable(
			name = "db_vers_user_perm",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "perm_id")
	)
	@BatchSize(size = 9)
	@Fetch(FetchMode.JOIN)
	@OptimisticLock(excluded = true)
	private Set<Permission> permissions;

	public User() {
	}

	public User(Integer id, String username) {
		this.id = id;
		this.username = username;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}
}
