//$Id: Node.java 10759 2006-11-08 00:00:53Z steve.ebersole@jboss.com $
package org.hibernate.test.ops;
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Node {

	private String name;
	private String description;
	private Date created;
	private Node parent;
	private Set children = new HashSet();
	private Set cascadingChildren = new HashSet();

	public Node() {
	}

	public Node(String name) {
		this.name = name;
		created = generateCurrentDate();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}

	public Node addChild(Node child) {
		children.add( child );
		child.setParent( this );
		return this;
	}

	public Set getCascadingChildren() {
		return cascadingChildren;
	}

	public void setCascadingChildren(Set cascadingChildren) {
		this.cascadingChildren = cascadingChildren;
	}

	private Date generateCurrentDate() {
		// Note : done as java.sql.Date mainly to work around issue with
		// MySQL and its lack of milli-second precision on its DATETIME
		// and TIMESTAMP datatypes.
		return new Date( new java.util.Date().getTime() );
	}
}
