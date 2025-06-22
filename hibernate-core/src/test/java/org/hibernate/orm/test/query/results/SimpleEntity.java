/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "SimpleEntity")
@Table(name = "simple_entity")
@NamedQuery(name= Queries.NAMED_ENTITY, query = Queries.ENTITY)
@NamedQuery(name= Queries.NAMED_ENTITY_NO_SELECT, query = Queries.ENTITY_NO_SELECT)
@NamedQuery(name= Queries.NAMED_COMPOSITE, query = Queries.COMPOSITE)
@NamedQuery(name= Queries.NAMED_NAME, query = Queries.NAME)
@NamedQuery(name= Queries.NAMED_COMP_VAL, query = Queries.COMP_VAL)
@NamedQuery(name= Queries.NAMED_ID_NAME, query = Queries.ID_NAME)
@NamedQuery(name= Queries.NAMED_ID_COMP_VAL, query = Queries.ID_COMP_VAL)
@NamedQuery(name= Queries.NAMED_ID_NAME_DTO, query = Queries.ID_NAME_DTO)
@NamedQuery(name= Queries.NAMED_ID_COMP_VAL_DTO, query = Queries.ID_COMP_VAL_DTO)
@NamedQuery(name= Queries.NAMED_NAME_DTO, query = Queries.NAME_DTO)
@NamedQuery(name= Queries.NAMED_COMP_VAL_DTO, query = Queries.COMP_VAL_DTO)
public class SimpleEntity {
	@Id
	public Integer id;
	public String name;
	public SimpleComposite composite;

	public SimpleEntity() {
	}

	public SimpleEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public SimpleEntity(Integer id, String name, SimpleComposite composite) {
		this.id = id;
		this.name = name;
		this.composite = composite;
	}
}
