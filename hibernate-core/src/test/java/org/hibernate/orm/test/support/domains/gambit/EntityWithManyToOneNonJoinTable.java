/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.support.domains.gambit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Chris Cranford
 */
@Entity
public class EntityWithManyToOneNonJoinTable {
	private Integer id;
	private EntityWithOneToManyNotOwned owner;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne
	public EntityWithOneToManyNotOwned getOwner() {
		return owner;
	}

	public void setOwner(EntityWithOneToManyNotOwned owner) {
		this.owner = owner;
	}
}
