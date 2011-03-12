/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.quote.resultsetmappings;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@SqlResultSetMappings({
		@SqlResultSetMapping(
				name="explicitScalarResultSetMapping",
				columns={ @ColumnResult(name="QuotEd_nAMe") }
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
