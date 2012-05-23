//$Id: Parent.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.unidir.manytoone;

/**
 * @author Gavin King
 */
public class Parent {
	private String name;
	Parent() {}
	public Parent(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
