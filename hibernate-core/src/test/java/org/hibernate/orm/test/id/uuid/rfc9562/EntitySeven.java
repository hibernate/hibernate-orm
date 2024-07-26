/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id.uuid.rfc9562;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.id.uuid.UuidVersion7Strategy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "EntitySeven")
@Table(name = "entity_seven")
public class EntitySeven {
	@Id
	@UuidGenerator(algorithm = UuidVersion7Strategy.class)
	public UUID id;
	@Basic
	public String name;

	private EntitySeven() {
		// for Hibernate use
	}

	public EntitySeven(String name) {
		this.name = name;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
