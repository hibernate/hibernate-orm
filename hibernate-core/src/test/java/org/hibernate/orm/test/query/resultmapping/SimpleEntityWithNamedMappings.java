/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import jakarta.persistence.Basic;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Steve Ebersole
 */
@Entity
@SqlResultSetMapping(
		name = "name",
		columns = @ColumnResult( name = "name" )
)
@SqlResultSetMapping(
		name = "id_name",
		columns = {
				@ColumnResult( name = "id" ),
				@ColumnResult( name = "name" )
		}
)
@SqlResultSetMapping(
		name = "id_name_dto",
		classes = @ConstructorResult(
				targetClass = SimpleEntityWithNamedMappings.DropDownDto.class,
				columns = {
						@ColumnResult( name = "id" ),
						@ColumnResult( name = "name" )
				}
		)
)
@SqlResultSetMapping(
		name = "entity-id-name",
		entities = @EntityResult(
				entityClass = SimpleEntityWithNamedMappings.class,
				fields = {
						@FieldResult( name = "id", column = "id" ),
						@FieldResult( name = "name", column = "name" )
				}
		)
)
@SqlResultSetMapping(
		name = "entity-none",
		entities = @EntityResult(
				entityClass = SimpleEntityWithNamedMappings.class
		)
)
@SqlResultSetMapping(
		name = "entity-lockmode",
		entities = @EntityResult(
				entityClass = SimpleEntityWithNamedMappings.class,
				lockMode = LockModeType.PESSIMISTIC_WRITE
		)
)
@SqlResultSetMapping(
		name = "entity-id-notes",
		entities = @EntityResult(
				entityClass = SimpleEntityWithNamedMappings.class,
				fields = {
						@FieldResult( name = "id", column = "id" ),
						@FieldResult( name = "notes", column = "notes" )
				}
		)
)
@SqlResultSetMapping(
		name = "entity",
		entities = @EntityResult(
				entityClass = SimpleEntityWithNamedMappings.class,
				fields = {
						@FieldResult( name = "id", column = "id" ),
						@FieldResult( name = "name", column = "name" ),
						@FieldResult( name = "notes", column = "notes" )
				}
		)
)
public class SimpleEntityWithNamedMappings {
	@Id
	private Integer id;

	private String name;
	private String notes;

	protected SimpleEntityWithNamedMappings() {
	}

	public SimpleEntityWithNamedMappings(Integer id, String name) {
		this( id, name, null );
	}

	public SimpleEntityWithNamedMappings(Integer id, String name, String notes) {
		this.id = id;
		this.name = name;
		this.notes = notes;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Basic( fetch = FetchType.LAZY )
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public static class DropDownDto {
		private final Integer id;
		private final String text;

		public DropDownDto(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}
}
