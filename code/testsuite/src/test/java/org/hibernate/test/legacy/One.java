//$Id: One.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

import java.util.Set;

public class One {
	Long key;
	String value;
	Set manies;
	private int x;
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	
	public void setKey(Long key) {
		this.key = key;
	}
	
	public Long getKey() {
		return this.key;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public Set getManies() {
		return manies;
	}
	
	public void setManies(Set manies) {
		this.manies = manies;
	}
	
}






