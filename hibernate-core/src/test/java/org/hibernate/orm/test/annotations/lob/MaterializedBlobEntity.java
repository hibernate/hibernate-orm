/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class MaterializedBlobEntity {
	@Id()
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	private String name;

	@Lob
	private byte[] theBytes;

	public MaterializedBlobEntity() {
	}

	public MaterializedBlobEntity(String name, byte[] theBytes) {
		this.name = name;
		this.theBytes = theBytes;
	}

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

	public byte[] getTheBytes() {
		return theBytes;
	}

	public void setTheBytes(byte[] theBytes) {
		this.theBytes = theBytes;
	}
}
