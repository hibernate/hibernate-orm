/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.cid;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.annotations.NaturalId;

/**
 * @author Donnchadh O Donnabhain
 */
@Entity
public class A {
	@EmbeddedId
	private AId accountId;
	@NaturalId(mutable = false)
	private String shortCode;

	protected A() {
	}

	public A(AId accountId, String shortCode) {
		this.accountId = accountId;
		this.shortCode = shortCode;
	}
	public String getShortCode() {
		return shortCode;
	}
	public AId getAccountId() {
		return accountId;
	}
}
