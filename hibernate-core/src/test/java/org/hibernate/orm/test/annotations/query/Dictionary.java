/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorColumn(name = "disc")
@DiscriminatorValue("Dic")
@SqlResultSetMapping(
		name = "dictionary", entities = {
@EntityResult(
		entityClass = Dictionary.class,
		fields = {
		@FieldResult(name = "id", column = "id"),
		@FieldResult(name = "name", column = "name"),
		@FieldResult(name = "editor", column = "editor")
				},
		discriminatorColumn = "`type`"
)
		}
)
@NamedNativeQuery(name = "all.dictionaries",
		query = "select id, name, editor, disc as \"type\" from Dictionary",
		resultSetMapping = "dictionary")
public class Dictionary {
	private Integer id;
	private String name;
	private String editor;

	@Id
	@GeneratedValue
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

	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}
}
