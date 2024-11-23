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
public class PhoneNumber {
	private int areaCode;
	private int prefix;
	private int lineNumber;

	private Classification classification;

	public PhoneNumber() {
	}

	public PhoneNumber(int areaCode, int prefix, int lineNumber, Classification classification) {
		this.areaCode = areaCode;
		this.prefix = prefix;
		this.lineNumber = lineNumber;
		this.classification = classification;
	}

	public int getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(int areaCode) {
		this.areaCode = areaCode;
	}

	public int getPrefix() {
		return prefix;
	}

	public void setPrefix(int prefix) {
		this.prefix = prefix;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	public enum Classification {
		HOME,
		WORK,
		MOBILE,
		MAIN,
		FAX,
		OTHER
	}
}
