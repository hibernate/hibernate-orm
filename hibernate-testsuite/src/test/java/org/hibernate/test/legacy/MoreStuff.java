//$Id: MoreStuff.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Collection;


public class MoreStuff implements Serializable {
	private String stringId;
	private int intId;
	private Collection stuffs;
	private String name;
	
	public boolean equals(Object other) {
		return ( (MoreStuff) other ).getIntId()==intId && ( (MoreStuff) other ).getStringId().equals(stringId);
	}
	
	public int hashCode() {
		return stringId.hashCode();
	}
	
	/**
	 * Returns the stuffs.
	 * @return Collection
	 */
	public Collection getStuffs() {
		return stuffs;
	}
	
	/**
	 * Sets the stuffs.
	 * @param stuffs The stuffs to set
	 */
	public void setStuffs(Collection stuffs) {
		this.stuffs = stuffs;
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
	 * Returns the intId.
	 * @return int
	 */
	public int getIntId() {
		return intId;
	}
	
	/**
	 * Returns the stringId.
	 * @return String
	 */
	public String getStringId() {
		return stringId;
	}
	
	/**
	 * Sets the intId.
	 * @param intId The intId to set
	 */
	public void setIntId(int intId) {
		this.intId = intId;
	}
	
	/**
	 * Sets the stringId.
	 * @param stringId The stringId to set
	 */
	public void setStringId(String stringId) {
		this.stringId = stringId;
	}
	
}






