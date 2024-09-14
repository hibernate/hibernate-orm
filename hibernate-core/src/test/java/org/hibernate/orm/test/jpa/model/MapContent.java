/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class MapContent {

	@Id
	private Long id;
	@ManyToOne(optional = false)
	private Relationship relationship;
	@ManyToOne
	private Relationship relationship2;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Relationship getRelationship() {
		return relationship;
	}
	public void setRelationship(Relationship relationship) {
		this.relationship = relationship;
	}

}
