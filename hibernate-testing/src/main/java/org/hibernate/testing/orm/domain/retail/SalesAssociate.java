/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "ASSOCIATE")
public class SalesAssociate {
	private Integer id;

	private Name name;

	public SalesAssociate() {
	}

	public SalesAssociate(Integer id, Name name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}
}
