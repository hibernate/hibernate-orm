/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.components.joins;

import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@jakarta.persistence.Entity
public class Entity {
	@Id
	@GeneratedValue
	private Long id;

	@Embedded
	private EmbeddedType embeddedType;

	public Entity() {
		// for jpa
	}

	public Entity(EmbeddedType embeddedType) {
		this.embeddedType = embeddedType;
	}

	public EmbeddedType getEmbeddedType() {
		return embeddedType;
	}

	public void setEmbeddedType(EmbeddedType embeddedType) {
		this.embeddedType = embeddedType;
	}
}
