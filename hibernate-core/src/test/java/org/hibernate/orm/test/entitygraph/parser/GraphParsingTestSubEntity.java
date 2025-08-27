/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;

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
