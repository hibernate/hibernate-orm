package org.hibernate.test.collection.list;
import java.util.ArrayList;
import java.util.List;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ListOwner {
	private String name;
	private ListOwner parent;
	private List children = new ArrayList();

	public ListOwner() {
	}

	public ListOwner(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ListOwner getParent() {
		return parent;
	}

	public void setParent(ListOwner parent) {
		this.parent = parent;
	}

	public List getChildren() {
		return children;
	}

	public void setChildren(List children) {
		this.children = children;
	}
}
