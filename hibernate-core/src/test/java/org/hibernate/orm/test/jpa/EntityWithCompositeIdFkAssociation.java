/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 */
@Entity
@Table(name = "entity_composite_fk")
public class EntityWithCompositeIdFkAssociation implements Serializable {

	@Id
	private int id;
	@ManyToOne
	private EntityWithCompositeId association;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public EntityWithCompositeId getAssociation() {
		return association;
	}

	public void setAssociation(EntityWithCompositeId association) {
		this.association = association;
	}
}
