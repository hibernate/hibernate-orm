/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.primarykeyjoincolumn;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Account implements Serializable {
	@Id
	@Column(name = "ACCOUNT_ID")
	@GeneratedValue(generator = "AccountForeignKeyGenerator")
	@GenericGenerator(name = "AccountForeignKeyGenerator", strategy = "foreign",
					parameters = {@Parameter(name = "property", value = "owner")})
	private Long accountId;

	private String type;

	@OneToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "ACCOUNT_ID", referencedColumnName = "PERSON_ID")
	private Person owner;

	public Account() {
	}

	public Account(String type) {
		this.type = type;
	}

	public Account(Long accountId, String type) {
		this.accountId = accountId;
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Account) ) {
			return false;
		}

		Account account = (Account) o;

		if ( accountId != null ? !accountId.equals( account.accountId ) : account.accountId != null ) {
			return false;
		}
		if ( type != null ? !type.equals( account.type ) : account.type != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = accountId != null ? accountId.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Account(accountId = " + accountId + ", type = " + type + ")";
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
