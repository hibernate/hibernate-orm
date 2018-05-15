/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cid;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jacob Robertson
 */
public class PurchaseRecord {
	public static class Id implements Serializable {
		private int purchaseNumber;
		private String purchaseSequence;

		public Id(int purchaseNumber, String purchaseSequence) {
			this.purchaseNumber = purchaseNumber;
			this.purchaseSequence = purchaseSequence;
		}
		public Id() {}

		/**
		 * @return Returns the purchaseNumber.
		 */
		public int getPurchaseNumber() {
			return purchaseNumber;
		}
		/**
		 * @param purchaseNumber The purchaseNumber to set.
		 */
		public void setPurchaseNumber(int purchaseNumber) {
			this.purchaseNumber = purchaseNumber;
		}
		/**
		 * @return the purchaseSequence
		 */
		public String getPurchaseSequence() {
			return purchaseSequence;
		}
		/**
		 * @param purchaseSequence the purchaseSequence to set
		 */
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

	private Id id;
	private Date timestamp = new Date();
	private Set details = new HashSet();

	public PurchaseRecord() {}

	/**
	 * @return Returns the id.
	 */
	public Id getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Id id) {
		this.id = id;
	}

	/**
	 * @return the details
	 */
	public Set getDetails() {
		return details;
	}

	/**
	 * @param details the details to set
	 */
	public void setDetails(Set details) {
		this.details = details;
	}

	/**
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
