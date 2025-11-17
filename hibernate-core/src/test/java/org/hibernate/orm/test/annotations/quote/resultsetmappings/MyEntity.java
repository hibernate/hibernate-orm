/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.quote.resultsetmappings;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@SqlResultSetMappings({
		@SqlResultSetMapping(
				name="explicitScalarResultSetMapping",
				columns= @ColumnResult( name = "\"QuotEd_nAMe\"" )
		)
		,
		@SqlResultSetMapping(
				name="basicEntityResultSetMapping",
				entities = @EntityResult( entityClass = MyEntity.class )
		)
		,
		@SqlResultSetMapping(
				name="expandedEntityResultSetMapping",
				entities = @EntityResult(
						entityClass = MyEntity.class,
						fields = {
								@FieldResult( name = "id", column = "eId" ),
								@FieldResult( name = "name", column = "eName" )
						}
				)
		)
})
@Entity
@Table( name = "MY_ENTITY_TABLE" )
public class MyEntity {
	private Long id;
	private String name;

	public MyEntity() {
	}

	public MyEntity(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column( name = "NAME" )
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
