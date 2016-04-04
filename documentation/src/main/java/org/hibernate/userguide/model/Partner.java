/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
//tag::hql-examples-domain-model-example[]
@Entity
public class Partner {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Version
	private int version;

	//Getters and setters are omitted for brevity

//end::hql-examples-domain-model-example[]
	public Partner() {
	}

	public Partner(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]