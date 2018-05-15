/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.access;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class AttributeAccessorEntity {
	private Integer id;
	private String name;

	public AttributeAccessorEntity() {

	}

	public AttributeAccessorEntity(String name) {
		this.name = name;
	}

	public AttributeAccessorEntity(Integer id, String name) {
		this( name );
		this.id = id;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@AttributeAccessor("org.hibernate.envers.test.support.domains.access.SimpleAttributeAccessorImpl")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "AttributeAccessorEntity{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}
}
