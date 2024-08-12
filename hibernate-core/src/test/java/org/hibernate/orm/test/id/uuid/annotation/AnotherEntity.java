/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.id.uuid.annotation;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;

/**
 * @author Steve Ebersole
 */
@Entity
public class AnotherEntity {
	@Id
	@GeneratedValue
	@UuidGenerator( algorithm = CustomUuidValueGenerator.class )
	private UUID id;
	@Basic
	private String name;

	protected AnotherEntity() {
		// for Hibernate use
	}

	public AnotherEntity(String name) {
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
