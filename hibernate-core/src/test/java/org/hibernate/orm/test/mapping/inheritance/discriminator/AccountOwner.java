/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jan Schatteman
 */
public class AccountOwner {
	private Long id;
	private String description;
	private Set<AbstractAccount> creditAccounts = new HashSet<>();
	private Set<AbstractAccount> debitAccounts = new HashSet<>();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<AbstractAccount> getCreditAccounts() {
		return creditAccounts;
	}

	public void setCreditAccounts(Set<AbstractAccount> creditAccounts) {
		this.creditAccounts = creditAccounts;
	}

	public Set<AbstractAccount> getDebitAccounts() {
		return debitAccounts;
	}

	public void setDebitAccounts(Set<AbstractAccount> debitAccounts) {
		this.debitAccounts = debitAccounts;
	}
}
