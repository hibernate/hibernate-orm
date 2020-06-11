package org.hibernate.orm.test.loading.entitygraph.parser;

import javax.persistence.Basic;
import javax.persistence.Entity;

@Entity( name = "GraphParsingTestSubEntity" )
public class GraphParsingTestSubEntity extends GraphParsingTestEntity {

	private String sub;

	@Basic
	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

}
