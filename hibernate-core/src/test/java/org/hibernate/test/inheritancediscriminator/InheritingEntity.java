/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritancediscriminator;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
