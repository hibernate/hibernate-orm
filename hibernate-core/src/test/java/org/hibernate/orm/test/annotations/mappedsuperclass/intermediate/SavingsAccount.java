/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mappedsuperclass.intermediate;
import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * The "leaf" entity in the hierarchy
 *
 * @author Saša Obradović
 */
@Entity
@Table(name = "SAVINGS_ACCOUNT")
@PrimaryKeyJoinColumn(name = "SAVACC_ACC_ID")
public class SavingsAccount extends SavingsAccountBase {
	public SavingsAccount() {
	}

	public SavingsAccount(String accountNumber, BigDecimal withdrawalLimit) {
		super( accountNumber, withdrawalLimit );
	}
}
