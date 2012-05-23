//$Id: Child.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.unidir.manytoone;



/**
 * @author Gavin King
 */
public class Child {
	private String name;
	private Parent parent;

	Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Parent getParent() {
		return parent;
	}
	
	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
