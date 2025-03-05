/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mappedsuperclass.intermediate;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Represents the most base super class in the hierarchy.
 *
 * @author Saša Obradović
 */
@MappedSuperclass
public abstract class AccountBase {
	@Id
	@org.hibernate.annotations.GenericGenerator(name = "generator::Account", strategy = "increment")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "generator::Account")
	@Column(name = "ACC_ID")
	private Long id;

	@Column(name = "ACC_NO")
	private String accountNumber;

	public Long getId() {
		return id;
	}

	protected AccountBase() {
	}

	protected AccountBase(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
}
