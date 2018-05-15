/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.mappedsuperclass.intermediate;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

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
