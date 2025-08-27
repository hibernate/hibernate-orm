/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idbag;

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
