/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.composite;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity(name = "PostalCarrier")
public class PostalCarrier {
	@Id
	private Long id;

	@NaturalId
	@Embedded
	private PostalCode postalCode;

	public PostalCarrier() {
	}

	public PostalCarrier(long id, PostalCode postalCode) {
		this.id = id;
		this.postalCode = postalCode;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PostalCode getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(PostalCode postalCode) {
		this.postalCode = postalCode;
	}
}
