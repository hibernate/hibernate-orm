/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id.uuid.custom;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity(name = "TheOtherEntity")
@Table(name = "TheOtherEntity")
public class TheOtherEntity {
	@Id @GeneratedValue
	public Long pk;

	@UuidGenerator(valueGenerator = UuidV6ValueGenerator.class)
	public UUID id;

	@Basic
	public String name;

	private TheOtherEntity() {
		// for Hibernate use
	}

	public TheOtherEntity(String name) {
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