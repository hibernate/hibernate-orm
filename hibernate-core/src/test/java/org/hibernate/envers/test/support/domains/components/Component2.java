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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Component2 that = (Component2) o;
		return Objects.equals( str5, that.str5 ) &&
				Objects.equals( str6, that.str6 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( str5, str6 );
	}

	@Override
	public String toString() {
		return "Component2{" +
				"str5='" + str5 + '\'' +
				", str6='" + str6 + '\'' +
				'}';
	}
}
