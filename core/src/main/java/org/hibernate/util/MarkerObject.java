//$Id: MarkerObject.java 4649 2004-10-07 00:14:29Z oneovthafew $
package org.hibernate.util;

/**
 * @author Gavin King
 */
public class MarkerObject {
	private String name;
	
	public MarkerObject(String name) {
		this.name=name;
	}
	public String toString() {
		return name;
	}
}
