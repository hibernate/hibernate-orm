package org.hibernate.test.collection.idbag;
import java.util.ArrayList;
import java.util.List;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class IdbagOwner {
	private String name;
	private List children = new ArrayList();

	public IdbagOwner() {
	}

	public IdbagOwner(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List getChildren() {
		return children;
	}

	public void setChildren(List children) {
		this.children = children;
	}
}
