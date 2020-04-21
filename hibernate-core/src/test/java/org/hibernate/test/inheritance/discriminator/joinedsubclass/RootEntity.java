/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Andrea Boriero
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn()
@DiscriminatorValue("ROOT")
public class RootEntity {
	@Id
	@GeneratedValue
	private Long id;

	public Long getId() {
		return id;
	}
}
