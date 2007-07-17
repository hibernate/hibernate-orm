//$Id: Parent.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

public class Parent {
	private long id;
	private int count;
	private Child child;
	private Object any;
	private int x;
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	
	public int getCount() {
		return count;
	}
	
	
	public long getId() {
		return id;
	}
	
	
	public void setCount(int count) {
		this.count = count;
	}
	
	
	public void setId(long id) {
		this.id = id;
	}
	
	
	public Child getChild() {
		return child;
	}
	
	
	public void setChild(Child child) {
		this.child = child;
	}
	
	public Object getAny() {
		return any;
	}

	public void setAny(Object any) {
		this.any = any;
	}

}







