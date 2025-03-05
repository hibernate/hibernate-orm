/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.idclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Gail Badner
 */
@Entity
@Table( name = "TOOL" )
public class Tool extends Helper {
	@Column( name = "COST" )
	private Double cost;
	public Double getCost( ) { return this.cost; }
	public void setCost( Double value ) { this.cost = value; }

	@Override
	public String toString( ) {
		return "[" + super.toString() + "; Cost: " + this.getCost( ) + "]";
	}
}
