/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.map; /**
 * @author Steve Ebersole
 */

import java.util.Map;

import org.hibernate.orm.test.mapping.collections.classification.Name;
import org.hibernate.orm.test.mapping.collections.classification.Status;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

//tag::collections-map-ex[]
@Entity
public class EntityWithMap {
	// ...
//end::collections-map-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-map-ex[]
	@ElementCollection
	private Map<Name, Status> names;
	//end::collections-map-ex[]

	private EntityWithMap() {
		// for Hibernate use
	}

	public EntityWithMap(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//tag::collections-map-ex[]
}
//end::collections-map-ex[]
