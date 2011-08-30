/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
