/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components;

import jakarta.persistence.Embeddable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class Component2 {
	private String str5;

	private String str6;

	public Component2(String str5, String str6) {
		this.str5 = str5;
		this.str6 = str6;
	}

	public Component2() {
	}

	public String getStr5() {
		return str5;
	}

	public void setStr5(String str5) {
		this.str5 = str5;
	}

	public String getStr6() {
		return str6;
	}

	public void setStr6(String str6) {
		this.str6 = str6;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Component2) ) {
			return false;
		}

		Component2 that = (Component2) o;

		if ( str5 != null ? !str5.equals( that.str5 ) : that.str5 != null ) {
			return false;
		}
		if ( str6 != null ? !str6.equals( that.str6 ) : that.str6 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (str5 != null ? str5.hashCode() : 0);
		result = 31 * result + (str6 != null ? str6.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "Comp2(str1 = " + str5 + ", " + str6 + ")";
	}
}
