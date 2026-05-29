/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;

import java.util.Date;
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
import org.hibernate.annotations.SourceType;

/**
 * Implementation of Group.
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "syb_ts_group")
public class Group {
	@Id
	@Column(name = "group_id")
	private Integer id;

	@Version
	@CurrentTimestamp(source = SourceType.DB)
	@Column(name = "ts")
	private Date timestamp;

	@Column(name = "name", unique = true)
	private String name;

	@ManyToMany
	@JoinTable(
			name = "syb_ts_user_group",
			joinColumns = @JoinColumn(name = "group_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id")
	)
	@BatchSize(size = 9)
	@Fetch(FetchMode.JOIN)
	private Set<User> users;

	public Group() {
	}

	public Group(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	protected void setId(Integer id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}
}
