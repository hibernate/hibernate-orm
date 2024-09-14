/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.userguide.tooling;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "customers")
//tag::tooling-modelgen-model[]
@Entity
public class Customer {
	@Id
	private Integer id;
	@Basic
	private String name;

	// getters and setters omitted for brevity
//end::tooling-modelgen-model[]

	private Customer() {
		// for Hibernate use
	}

	public Customer(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
//tag::tooling-modelgen-model[]
}
//end::tooling-modelgen-model[]
