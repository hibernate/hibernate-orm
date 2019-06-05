/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import javax.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
@SuppressWarnings("unused")
public class Name {
	private String familyName;
	private String familiarName;

	private String prefix;
	private String suffix;

	public Name() {
	}

	public Name(String familyName, String familiarName) {
		this.familyName = familyName;
		this.familiarName = familiarName;
	}

	public Name(String familyName, String familiarName, String suffix) {
		this.familyName = familyName;
		this.familiarName = familiarName;
		this.suffix = suffix;
	}

	public Name(String familyName, String familiarName, String prefix, String suffix) {
		this.familyName = familyName;
		this.familiarName = familiarName;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getFamiliarName() {
		return familiarName;
	}

	public void setFamiliarName(String familiarName) {
		this.familiarName = familiarName;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
}
