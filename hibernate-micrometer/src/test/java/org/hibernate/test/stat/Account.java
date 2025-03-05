/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.stat;

import jakarta.persistence.Basic;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

@Entity
@Table( name = "t_acct" )
public class Account {
	@EmbeddedId
	private AccountId accountId;

	@Basic( optional = false )
	@NaturalId
	private String shortCode;

	protected Account() {
	}

	public Account(AccountId accountId, String shortCode) {
		this.accountId = accountId;
		this.shortCode = shortCode;
	}

	public AccountId getAccountId() {
		return accountId;
	}

	public String getShortCode() {
		return shortCode;
	}
}
