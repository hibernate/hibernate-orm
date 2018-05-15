/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
