/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.identifier.uuid.custom2;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.orm.test.mapping.identifier.uuid.custom.CustomUuidValueCreator;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::example-identifiers-generators-uuid-implicit[]
@Entity
public class Book {
	@Id
	@GeneratedValue
	@UuidGenerator(algorithm = CustomUuidValueCreator.class)
	private String id;
	@Basic
	private String name;

	//end::example-identifiers-generators-uuid-implicit[]
	protected Book() {
		// for Hibernate use
	}

	public Book(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//tag::example-identifiers-generators-uuid-implicit[]
}
//end::example-identifiers-generators-uuid-implicit[]
