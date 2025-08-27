/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;

/**
 * @author Strong Liu
 */
@Entity
@NamedStoredProcedureQueries(
		{
				@NamedStoredProcedureQuery(
						name = "s1",
						procedureName = "p1",
						parameters = {
								@StoredProcedureParameter(name = "p11",
										mode = ParameterMode.IN,
										type = Integer.class),
								@StoredProcedureParameter(name = "p12",
										mode = ParameterMode.IN,
										type = Integer.class
								)
						},
						resultClasses = { User.class }
				),
				@NamedStoredProcedureQuery(
						name = "s2",
						procedureName = "p2",
						parameters = {
								@StoredProcedureParameter(
										mode = ParameterMode.INOUT,
										type = String.class),
								@StoredProcedureParameter(
										mode = ParameterMode.INOUT,
										type = Long.class)
						},
						resultSetMappings = { "srms" }

				),
				@NamedStoredProcedureQuery(
						name = "positional-param",
						procedureName = "positionalParameterTesting",
						parameters = {
								@StoredProcedureParameter( mode = ParameterMode.IN, type = Integer.class )
						}
				)
		}
)
@SqlResultSetMapping(name = "srms",
		entities = {
				@EntityResult(entityClass = User.class, fields = {
						@FieldResult(name = "id", column = "order_id"),
						@FieldResult(name = "name", column = "order_item")
				})
		}
)
@Table( name = "T_USER" )
public class User {
	@Id
	private int id;
	private String name;

	public User() {
	}

	public User(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
