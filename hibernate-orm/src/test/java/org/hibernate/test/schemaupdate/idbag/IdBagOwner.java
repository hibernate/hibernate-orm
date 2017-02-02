/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.idbag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrea Boriero
 */
public class IdBagOwner {
	private long id;
	private List<IdBagOwner> children = new ArrayList<IdBagOwner>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<IdBagOwner> getChildren() {
		return children;
	}

	public void setChildren(List<IdBagOwner> children) {
		this.children = children;
	}

	public void addChild(IdBagOwner child){
		children.add( child );
	}
}
