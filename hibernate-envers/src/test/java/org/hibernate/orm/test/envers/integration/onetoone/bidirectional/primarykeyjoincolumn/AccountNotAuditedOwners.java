/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.primarykeyjoincolumn;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class AccountNotAuditedOwners implements Serializable {
	@Id
	@Column(name = "ACCOUNT_ID")
	@GeneratedValue
	private Long accountId;

	private String type;

	@OneToOne(mappedBy = "account", optional = false, fetch = FetchType.LAZY)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private NotAuditedPerson owner;

	public AccountNotAuditedOwners() {
	}

	public AccountNotAuditedOwners(String type) {
		this.type = type;
	}

	public AccountNotAuditedOwners(Long accountId, String type) {
		this.accountId = accountId;
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof AccountNotAuditedOwners) ) {
			return false;
		}

		AccountNotAuditedOwners account = (AccountNotAuditedOwners) o;

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
		return "AccountNotAuditedOwners(accountId = " + accountId + ", type = " + type + ")";
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public NotAuditedPerson getOwner() {
		return owner;
	}

	public void setOwner(NotAuditedPerson owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
