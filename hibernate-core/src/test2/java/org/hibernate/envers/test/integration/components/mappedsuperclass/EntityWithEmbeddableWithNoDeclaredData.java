/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.mappedsuperclass;


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Jakob Braeuchi.
 */
@Entity
@Table(name="EntEmbWNoDeclData")
@Access(AccessType.FIELD)
@Audited
public class EntityWithEmbeddableWithNoDeclaredData {

	@Id
	@GeneratedValue
	private long id;

	@Column(name = "NAME", length = 100)
	private String name;

	@Embedded
	private EmbeddableWithNoDeclaredData value;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EmbeddableWithNoDeclaredData getValue() {
		return value;
	}

	public void setValue(EmbeddableWithNoDeclaredData value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) ( id ^ ( id >>> 32 ) );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		EntityWithEmbeddableWithNoDeclaredData other = (EntityWithEmbeddableWithNoDeclaredData) obj;
		if ( id != other.id ) {
			return false;
		}
		return true;
	}
}
