package org.hibernate.test.extralazy;

import java.util.HashMap;
import java.util.Map;



public class Parent {

	private String id;
	
	private Map <String, Child> children = new HashMap<String, Child>  ();

	public void setChildren(Map <String, Child> children) {
		this.children = children;
	}

	public Map <String, Child> getChildren() {
		return children;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}	
}
