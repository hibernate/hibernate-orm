/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;
import java.util.Date;

/**
 * Implementation of Permission.
 *
 * @author Steve Ebersole
 */
public class Permission {
	private Long id;
	private Date timestamp;
	private String name;
	private String context;
	private String access;

	public Permission() {
	}

	public Permission(String name, String context, String access) {
		this.name = name;
		this.context = context;
		this.access = access;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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
