/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import java.util.HashSet;
import java.util.Set;


public class Tour {
	private Integer tourID;
	private long version;
	private String name;
	private Set<Node> nodes = new HashSet<>(0);

	public String getName() {
		return name;
	}

	public Integer getTourID() {
		return tourID;
	}

	protected void setTourID(Integer tourID) {
		this.tourID = tourID;
	}

	public long getVersion() {
		return version;
	}

	protected void setVersion(long version) {
		this.version = version;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}
}
