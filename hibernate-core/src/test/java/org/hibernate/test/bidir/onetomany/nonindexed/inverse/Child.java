//$Id: Child.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.bidir.onetomany.nonindexed.inverse;



/**
 * @author Gavin King
 */
public class Child {
	private String name;
	private int age;
	private Parent parent;

	Child() {
	}

	public Child(String name) {
		this( name, 0 );
	}

	public Child(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
