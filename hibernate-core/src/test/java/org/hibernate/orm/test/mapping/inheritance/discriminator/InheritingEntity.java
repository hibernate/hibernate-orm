/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Pawel Stawicki
 */
@Entity
@DiscriminatorValue("1")
public class InheritingEntity extends ParentEntity {
	public InheritingEntity() {
	}

	public InheritingEntity(String someValue) {
		this.someValue = someValue;
	}

	@Column(name = "dupa")
	private String someValue;

	public String getSomeValue() {
		return someValue;
	}

	public void setSomeValue(String someValue) {
		this.someValue = someValue;
	}
}
