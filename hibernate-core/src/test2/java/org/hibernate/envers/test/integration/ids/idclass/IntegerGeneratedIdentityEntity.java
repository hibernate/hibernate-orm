/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.idclass;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Matthew Morrissette (yinzara at gmail dot com)
 */
@Audited
@Entity(name = "IntegerGenIdEntity")
public class IntegerGeneratedIdentityEntity implements Serializable {

	@Id
	@GeneratedValue
	private Integer id;

	private String description;

	public IntegerGeneratedIdentityEntity() {
	}

	public IntegerGeneratedIdentityEntity(Integer id) {
		this.id = id;
	}

	public IntegerGeneratedIdentityEntity(String description) {
		this.description = description;
	}

	public IntegerGeneratedIdentityEntity(Integer id, String description) {
		this.id = id;
		this.description = description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !(o instanceof IntegerGeneratedIdentityEntity)) return false;

		IntegerGeneratedIdentityEntity that = (IntegerGeneratedIdentityEntity) o;

		return id != null ? id.equals(that.id) : that.id == null;

	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "IntegerGeneratedIdentityEntity(id = " + id + ", description = " + description + ")";
	}


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
