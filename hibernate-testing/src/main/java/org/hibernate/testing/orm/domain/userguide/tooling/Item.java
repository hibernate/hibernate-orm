/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.userguide.tooling;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "items")
//tag::tooling-modelgen-model[]
@Entity
public class Item {
	@Id
	Integer id;

	int quantity;

	@ManyToOne
	Order order;

	// getters and setters omitted for brevity
//end::tooling-modelgen-model[]

	public Item() {
	}

	public Item(Integer id, int quantity, Order order) {
		this.id = id;
		this.quantity = quantity;
		this.order = order;

		order.items.add( this );
	}

//tag::tooling-modelgen-model[]
}
//end::tooling-modelgen-model[]
