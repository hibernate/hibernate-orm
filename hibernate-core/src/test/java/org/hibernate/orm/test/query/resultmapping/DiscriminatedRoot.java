/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity( name = "DiscriminatedRoot" )
@Table( name = "discriminated_entity" )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "type_code", length = 20 )
@DiscriminatorValue( "root" )
@SqlResultSetMapping(
		name = "root-implicit",
		entities = @EntityResult(
				entityClass = DiscriminatedRoot.class
		)
)
@SqlResultSetMapping(
		name = "root-explicit",
		entities = @EntityResult(
				entityClass = DiscriminatedRoot.class,
				discriminatorColumn = "type_code_alias",
				fields = {
						@FieldResult( name = "id", column = "id_alias" ),
						@FieldResult( name = "rootName", column = "root_name_alias" ),
						@FieldResult( name = "subType1Name", column = "sub_type1_name_alias" ),
						@FieldResult( name = "subType2Name", column = "sub_type2_name_alias" )
				}
		)
)
class DiscriminatedRoot {
	private Integer id;
	private String rootName;

	public DiscriminatedRoot() {
	}

	public DiscriminatedRoot(Integer id, String rootName) {
		this.id = id;
		this.rootName = rootName;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( name = "root_name" )
	public String getRootName() {
		return rootName;
	}

	public void setRootName(String rootName) {
		this.rootName = rootName;
	}
}
