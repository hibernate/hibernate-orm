/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.db;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "db_vers_permission")
public class Permission {
	@Id
	@Column(name = "perm_id")
	private Integer id;

	@Version
	@CurrentTimestamp(source = SourceType.DB)
	@Column(name = "ts")
	private Date timestamp;

	@Column(name = "name", unique = true, nullable = false)
	private String name;

	@Column(name = "ctx")
	private String context;

	@Column(name = "priv")
	private String access;

	public Permission() {
	}

	public Permission(Integer id, String name, String context, String access) {
		this.id = id;
		this.name = name;
		this.context = context;
		this.access = access;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}
}
