/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.components;

import java.util.Objects;

import javax.persistence.Embeddable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class Component1 {
	private String str1;

	private String str2;

	public Component1(String str1, String str2) {
		this.str1 = str1;
		this.str2 = str2;
	}

	public Component1() {
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Component1 that = (Component1) o;
		return Objects.equals( str1, that.str1 ) &&
				Objects.equals( str2, that.str2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( str1, str2 );
	}

	@Override
	public String toString() {
		return "Component1{" +
				"str1='" + str1 + '\'' +
				", str2='" + str2 + '\'' +
				'}';
	}
}
