/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql.nullPrecedence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nathan Xu
 */
@Entity( name = "ExampleEntity" )
@Table( name = "ExampleEntity" )
public class ExampleEntity {
	@Id
	private Long id;

	private String name;

	public ExampleEntity() {
	}

	public ExampleEntity(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
