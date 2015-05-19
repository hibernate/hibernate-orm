/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.mappedsuperclass.intermediate;
import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

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
