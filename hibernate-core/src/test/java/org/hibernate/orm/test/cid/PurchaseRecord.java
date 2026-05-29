/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class PurchaseRecord {

	@Embeddable
	public static class Id implements Serializable {
		private int purchaseNumber;
		private String purchaseSequence;

		public Id(int purchaseNumber, String purchaseSequence) {
			this.purchaseNumber = purchaseNumber;
			this.purchaseSequence = purchaseSequence;
		}
		public Id() {}

		public int getPurchaseNumber() {
			return purchaseNumber;
		}

		public void setPurchaseNumber(int purchaseNumber) {
			this.purchaseNumber = purchaseNumber;
		}

		public String getPurchaseSequence() {
			return purchaseSequence;
		}

		public void setPurchaseSequence(String purchaseSequence) {
			this.purchaseSequence = purchaseSequence;
		}

		public int hashCode() {
			return purchaseNumber + purchaseSequence.hashCode();
		}

		public boolean equals(Object other) {
			if (other instanceof Id) {
				Id that = (Id) other;
				return purchaseSequence.equals(this.purchaseSequence) &&
					that.purchaseNumber == this.purchaseNumber;
			}
			else {
				return false;
			}
		}
	}

	@EmbeddedId
	@GenericGenerator(type = PurchaseRecordIdGenerator.class)
	private Id id;

	@Column(name = "`timestamp`")
	private Date timestamp;

	@OneToMany(mappedBy = "purchaseRecord", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<PurchaseDetail> details = new HashSet<>();

	public PurchaseRecord() {}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Set<PurchaseDetail> getDetails() {
		return details;
	}

	public void setDetails(Set<PurchaseDetail> details) {
		this.details = details;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
