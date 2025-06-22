/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.list;

import java.util.List;

import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

/**
 * @author Steve Ebersole
 */
//tag::collections-list-ordercolumn-ex[]
@Entity
public class EntityWithOrderColumnList {
	// ...
//end::collections-list-ordercolumn-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-list-ordercolumn-ex[]
	@ElementCollection
	@OrderColumn( name = "name_index" )
	private List<Name> names;
	//end::collections-list-ordercolumn-ex[]

	private EntityWithOrderColumnList() {
		// for Hibernate use
	}

	public EntityWithOrderColumnList(Integer id, String name) {
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

//tag::collections-list-ordercolumn-ex[]
}
//end::collections-list-ordercolumn-ex[]
