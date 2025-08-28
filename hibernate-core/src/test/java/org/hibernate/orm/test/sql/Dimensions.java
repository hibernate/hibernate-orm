/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 * @author Vlad MIhalcea
 */
//tag::sql-composite-key-entity-associations_named-query-example[]
@Embeddable
public class Dimensions {

	private int length;

	private int width;

	//Getters and setters are omitted for brevity

//end::sql-composite-key-entity-associations_named-query-example[]

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}
//tag::sql-composite-key-entity-associations_named-query-example[]
}
//end::sql-composite-key-entity-associations_named-query-example[]
