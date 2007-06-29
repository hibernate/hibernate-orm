//$Id$
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
