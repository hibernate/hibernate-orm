package org.hibernate.test.collection.bag;
import java.util.ArrayList;
import java.util.List;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class BagOwner {
	private String name;
	private BagOwner parent;
	private List children = new ArrayList();

	public BagOwner() {
	}

	public BagOwner(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BagOwner getParent() {
		return parent;
	}

	public void setParent(BagOwner parent) {
		this.parent = parent;
	}

	public List getChildren() {
		return children;
	}

	public void setChildren(List children) {
		this.children = children;
	}
}
