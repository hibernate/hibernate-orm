package org.hibernate.test.extralazy;



public class Child {

	private String id;
	
	private Parent parent;
	
	private String firstName;

	public void setParent(Parent parent) {
		this.parent = parent;
	}

	public Parent getParent() {
		return parent;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
