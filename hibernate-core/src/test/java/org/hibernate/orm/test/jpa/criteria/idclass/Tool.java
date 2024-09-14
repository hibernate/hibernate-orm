/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
