/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.mappedsuperclass;

import jakarta.persistence.MappedSuperclass;

/**
 * @author Andrea Boriero
 */
@MappedSuperclass
public class AbstractEntity {
	private Long id;

	private int version;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
