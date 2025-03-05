/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mappedsuperclass.intermediate;
import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;


/**
 * Represents the intermediate mapped superclass in the hierarchy.
 *
 * @author Saša Obradović
 */
@MappedSuperclass
public abstract class SavingsAccountBase extends Account {
	@Column(name = "SAVACC_WITHDRAWALLIMIT",
			precision = 8, scale = 2)
	private BigDecimal withdrawalLimit;

	protected SavingsAccountBase() {
	}

	protected SavingsAccountBase(String accountNumber, BigDecimal withdrawalLimit) {
		super( accountNumber );
		this.withdrawalLimit = withdrawalLimit;
	}

	public BigDecimal getWithdrawalLimit() {
		return withdrawalLimit;
	}

	public void setWithdrawalLimit(BigDecimal withdrawalLimit) {
		this.withdrawalLimit = withdrawalLimit;
	}
}
