/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import java.util.HashSet;
import java.util.Set;


public class Tour {

//	@Id
//	@SequenceGenerator(name="TOUR_SEQ", sequenceName="TOUR_SEQ", initialValue=1, allocationSize=1)
//	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="TOUR_SEQ")
	private Long tourID;

	private long version;

	private String name;

	/** A List of nodes contained in this tour. */
//	@OneToMany(targetEntity=Node.class, fetch=FetchType.LAZY, cascade={CascadeType.MERGE, CascadeType.REFRESH}, mappedBy="tour")
	private Set nodes = new HashSet(0);

	public String getName() {
		return name;
	}

	protected void setTourID(Long tourID) {
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

	public Set getNodes() {
		return nodes;
	}

	public void setNodes(Set nodes) {
		this.nodes = nodes;
	}

	public Long getTourID() {
		return tourID;
	}
}
