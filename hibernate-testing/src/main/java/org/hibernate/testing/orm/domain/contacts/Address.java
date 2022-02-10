/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.contacts;

import javax.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Address {
	private Classification classification;
	private String line1;
	private String line2;
	private PostalCode postalCode;

	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	public PostalCode getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(PostalCode postalCode) {
		this.postalCode = postalCode;
	}



	public enum Classification {
		HOME,
		WORK,
		MAIN,
		OTHER
	}

	@Embeddable
	public static class PostalCode {
		private int zipCode;
		private int plus4;

		public int getZipCode() {
			return zipCode;
		}

		public void setZipCode(int zipCode) {
			this.zipCode = zipCode;
		}

		public int getPlus4() {
			return plus4;
		}

		public void setPlus4(int plus4) {
			this.plus4 = plus4;
		}
	}
}
