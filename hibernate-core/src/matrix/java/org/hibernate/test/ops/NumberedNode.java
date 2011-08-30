//$Id: NumberedNode.java 7236 2005-06-20 03:19:34Z oneovthafew $
package org.hibernate.test.ops;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class NumberedNode {
	
	private long id;
	private String name;
	private NumberedNode parent;
	private Set children = new HashSet();
	private String description;
	private Date created;

	public NumberedNode() {
		super();
	}

	public NumberedNode(String name) {
		this.name = name;
		created = new Date();
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	public Set getChildren() {
		return children;
	}
	public void setChildren(Set children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public NumberedNode getParent() {
		return parent;
	}
	public void setParent(NumberedNode parent) {
		this.parent = parent;
	}
	
	public NumberedNode addChild(NumberedNode child) {
		children.add(child);
		child.setParent(this);
		return this;
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
}
