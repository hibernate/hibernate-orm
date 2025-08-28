/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components;

import jakarta.persistence.Embedded;

/**
 * @author Erik-Berndt Scheper
 */
public class DefaultValueComponent1 {

	private String str1;

	@Embedded
	private DefaultValueComponent2 comp2 = new DefaultValueComponent2();

	public static final DefaultValueComponent1 of(
			String str1,
			DefaultValueComponent2 comp2) {
		DefaultValueComponent1 instance = new DefaultValueComponent1();
		instance.setStr1( str1 );
		instance.setComp2( comp2 );
		return instance;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public DefaultValueComponent2 getComp2() {
		return comp2;
	}

	public void setComp2(DefaultValueComponent2 comp2) {
		this.comp2 = comp2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DefaultValueComponent1) ) {
			return false;
		}

		DefaultValueComponent1 that = (DefaultValueComponent1) o;

		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}
		if ( comp2 != null ? !comp2.equals( that.comp2 ) : that.comp2 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (str1 != null ? str1.hashCode() : 0);
		result = 31 * result + (comp2 != null ? comp2.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "Comp1(str1 = " + str1 + ", comp2 = " + comp2 + ")";
	}

}
