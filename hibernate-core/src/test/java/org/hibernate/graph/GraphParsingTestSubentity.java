package org.hibernate.graph;

import javax.persistence.Basic;
import javax.persistence.Entity;

@Entity
public class GraphParsingTestSubentity extends GraphParsingTestEntity {

	private String sub;

	@Basic
	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

}