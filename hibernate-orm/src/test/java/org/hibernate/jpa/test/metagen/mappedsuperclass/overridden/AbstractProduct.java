/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.overridden;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Oliver Breidenbach
 */
@MappedSuperclass
@Access(AccessType.PROPERTY)
public abstract class AbstractProduct {
	private Long id;
	private String name;

	protected AbstractProduct() {
	}

	protected AbstractProduct(String name) {
		this.name = name;
	}
	@Id
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
