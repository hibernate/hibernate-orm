/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.wildfly.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
public class AuditedEntity {
	@Id
	private Integer id;

	private String name;

	AuditedEntity() {

	}

	public AuditedEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null || !( object instanceof AuditedEntity ) ) {
			return false;
		}

		AuditedEntity that = (AuditedEntity) object;
		return !( name != null ? !name.equals( that.name ) : that.name != null );
	}

	@Override
	public int hashCode() {
		return ( name != null ? name.hashCode() : 0 );
	}
}
