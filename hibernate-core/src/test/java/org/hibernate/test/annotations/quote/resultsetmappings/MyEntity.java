/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
