/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.named.resultmapping;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;

/**
 * @author Steve Ebersole
 */
@Entity
@SqlResultSetMapping(
		name = "name",
		columns = @ColumnResult( name = "name" )
)
public class SimpleEntityWithNamedMappings {
	@Id
	private Integer id;

	private String name;

	protected SimpleEntityWithNamedMappings() {
	}

	public SimpleEntityWithNamedMappings(Integer id, String name) {
		this.id = id;
		this.name = name;
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
}
