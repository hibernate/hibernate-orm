/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.primarykeyjoincolumn;

import java.io.Serializable;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class NotAuditedNoProxyPerson implements Serializable {
	@Id
	@Column(name = "PERSON_ID")
	@GeneratedValue(generator = "NotAuditedNoProxyKeyGenerator")
	@GenericGenerator(name = "NotAuditedNoProxyKeyGenerator", strategy = "foreign",
					parameters = {@Parameter(name = "property", value = "account")})
	private Long personId;

	private String name;

	@OneToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "PERSON_ID", referencedColumnName = "ACCOUNT_ID")
	private AccountNotAuditedOwners account;

	public NotAuditedNoProxyPerson() {
	}

	public NotAuditedNoProxyPerson(String name) {
		this.name = name;
	}

	public NotAuditedNoProxyPerson(Long personId, String name) {
		this.personId = personId;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NotAuditedNoProxyPerson) ) {
			return false;
		}

		NotAuditedNoProxyPerson person = (NotAuditedNoProxyPerson) o;

		if ( personId != null ? !personId.equals( person.personId ) : person.personId != null ) {
			return false;
		}
		if ( name != null ? !name.equals( person.name ) : person.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = personId != null ? personId.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "NotAuditedNoProxyPerson(personId = " + personId + ", name = " + name + ")";
	}

	public Long getPersonId() {
		return personId;
	}

	public void setPersonId(Long personId) {
		this.personId = personId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AccountNotAuditedOwners getAccount() {
		return account;
	}

	public void setAccount(AccountNotAuditedOwners account) {
		this.account = account;
	}
}
