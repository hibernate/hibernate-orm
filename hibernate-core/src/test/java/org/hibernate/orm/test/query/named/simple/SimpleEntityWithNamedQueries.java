/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.named.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

/**
 * @author Steve Ebersole
 */
@Entity
@NamedQuery(
		name = "simple",
		query = "select e from SimpleEntityWithNamedQueries e"
)
@NamedQuery(
		name = "restricted",
		query = "select e from SimpleEntityWithNamedQueries e where e.name = :name"
)
public class SimpleEntityWithNamedQueries {
	@Id
	private Integer id;
	private String name;

	SimpleEntityWithNamedQueries() {
	}

	public SimpleEntityWithNamedQueries(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
