/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;


/**
 * Implementation of GeneratedPropertyEntity.
 *
 * @author Steve Ebersole
 */
public class GeneratedPropertyEntity {
	private Long id;
	private String name;
	private byte[] lastModified;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getLastModified() {
		return lastModified;
	}

	public void setLastModified(byte[] lastModified) {
		this.lastModified = lastModified;
	}
}
