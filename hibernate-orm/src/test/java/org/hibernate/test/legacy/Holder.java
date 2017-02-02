/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Holder.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.List;
import java.util.Set;

public class Holder implements Named {
	private String id;
	private List ones;
	private Foo[] fooArray;
	private Set foos;
	private String name;
	private Holder otherHolder;
	
	public Holder() {
	}
	public Holder(String name) {
		this.name=name;
	}
	
	/**
	 * Returns the fooArray.
	 * @return Foo[]
	 */
	public Foo[] getFooArray() {
		return fooArray;
	}
	
	/**
	 * Returns the foos.
	 * @return Set
	 */
	public Set getFoos() {
		return foos;
	}
	
	/**
	 * Sets the fooArray.
	 * @param fooArray The fooArray to set
	 */
	public void setFooArray(Foo[] fooArray) {
		this.fooArray = fooArray;
	}
	
	/**
	 * Sets the foos.
	 * @param foos The foos to set
	 */
	public void setFoos(Set foos) {
		this.foos = foos;
	}
	
	/**
	 * Returns the id.
	 * @return String
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the ones.
	 * @return List
	 */
	public List getOnes() {
		return ones;
	}
	
	/**
	 * Sets the ones.
	 * @param ones The ones to set
	 */
	public void setOnes(List ones) {
		this.ones = ones;
	}
	
	public Holder getOtherHolder() {
		return otherHolder;
	}

	public void setOtherHolder(Holder holder) {
		otherHolder = holder;
	}

}






