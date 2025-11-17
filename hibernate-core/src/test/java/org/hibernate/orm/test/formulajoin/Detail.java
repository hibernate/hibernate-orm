/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.formulajoin;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Detail implements Serializable {
	private Long id;
	private Root root;
	private int version;
	private String details;
	private boolean currentVersion;

	public boolean isCurrentVersion() {
		return currentVersion;
	}
	public void setCurrentVersion(boolean currentVersion) {
		this.currentVersion = currentVersion;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Root getRoot() {
		return root;
	}
	public void setRoot(Root root) {
		this.root = root;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
}
