/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Detail.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Detail implements Serializable {
	
	private Master master;
	private int i;
	private Set details = new HashSet();
	private int x;
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	
	public Master getMaster() {
		return master;
	}
	
	public void setMaster(Master master) {
		this.master = master;
	}
	
	public int getI() {
		return i;
	}
	
	public void setI(int i) {
		this.i = i;
	}
	
	/**
	 * Returns the details.
	 * @return Set
	 */
	public Set getSubDetails() {
		return details;
	}
	
	/**
	 * Sets the details.
	 * @param details The details to set
	 */
	public void setSubDetails(Set details) {
		this.details = details;
	}
	
}






