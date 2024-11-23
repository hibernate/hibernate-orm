/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Chris Cranford
 */
@Entity
public class EntityWithManyToOneWithoutJoinTable {
	private Integer id;
	private Integer someInteger;
	private EntityWithOneToManyNotOwned owner;

	EntityWithManyToOneWithoutJoinTable() {
	}

	public EntityWithManyToOneWithoutJoinTable(Integer id, Integer someInteger) {
		this.id = id;
		this.someInteger = someInteger;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	@ManyToOne
	public EntityWithOneToManyNotOwned getOwner() {
		return owner;
	}

	public void setOwner(EntityWithOneToManyNotOwned owner) {
		this.owner = owner;
	}
}
