/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob.locator;

import java.sql.Blob;
import java.sql.Clob;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Lukasz Antoniak
 */
@Entity
public class LobHolder {
	@Id
	@GeneratedValue
	private Long id;

	private Clob clobLocator;

	private Blob blobLocator;

	private Integer counter;

	public LobHolder() {
	}

	public LobHolder(Blob blobLocator, Clob clobLocator, Integer counter) {
		this.blobLocator = blobLocator;
		this.clobLocator = clobLocator;
		this.counter = counter;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Clob getClobLocator() {
		return clobLocator;
	}

	public void setClobLocator(Clob clobLocator) {
		this.clobLocator = clobLocator;
	}

	public Blob getBlobLocator() {
		return blobLocator;
	}

	public void setBlobLocator(Blob blobLocator) {
		this.blobLocator = blobLocator;
	}

	public Integer getCounter() {
		return counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}
}
