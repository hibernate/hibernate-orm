/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * Base class for testing joined inheritance with a discriminator column.
 *
 * @author Etienne Miret
 */
@Entity
@Inheritance( strategy = InheritanceType.JOINED )
@DiscriminatorColumn( name = "kind" )
public abstract class Polygon {

	@Id
	private Integer id;

	private String description;

	public Integer getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

}
