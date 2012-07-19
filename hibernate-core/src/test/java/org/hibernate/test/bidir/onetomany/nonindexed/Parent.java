//$Id: Parent.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.bidir.onetomany.nonindexed;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Parent {
	private String name;
	private Set<Child> children = new HashSet<Child>();
	Parent() {}
	public Parent(String name) {
		this.name = name;
	}
	public Set<Child> getChildren() {
		return children;
	}
	public void setChildren(Set<Child> children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
