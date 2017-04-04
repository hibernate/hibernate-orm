/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: C2.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.ArrayList;
import java.util.Collection;

public class C2 extends B {
	private String address;
	private C1 c1;
	private String c2Name;
	private Collection c1s = new ArrayList();
	/**
	 * Returns the address.
	 * @return String
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * Sets the address.
	 * @param address The address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	
	/**
	 * @return Returns the c.
	 */
	public C1 getC1() {
		return c1;
	}

	/**
	 * @param c The c to set.
	 */
	public void setC1(C1 c) {
		this.c1 = c;
	}

	/**
	 * @return Returns the cs.
	 */
	public Collection getC1s() {
		return c1s;
	}

	/**
	 * @param cs The cs to set.
	 */
	public void setC1s(Collection cs) {
		this.c1s = cs;
	}

	public String getC2Name() {
		return c2Name;
	}

	public void setC2Name(String name) {
		c2Name = name;
	}
}






