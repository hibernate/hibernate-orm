/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Custom detail of revision entity.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class ModifiedEntityTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@ManyToOne
	private CustomTrackingRevisionEntity revision;

	private String entityClassName;

	public ModifiedEntityTypeEntity() {
	}

	public ModifiedEntityTypeEntity(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public ModifiedEntityTypeEntity(CustomTrackingRevisionEntity revision, String entityClassName) {
		this.revision = revision;
		this.entityClassName = entityClassName;
	}

	public CustomTrackingRevisionEntity getRevision() {
		return revision;
	}

	public void setRevision(CustomTrackingRevisionEntity revision) {
		this.revision = revision;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public void setEntityClassName(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ModifiedEntityTypeEntity) ) {
			return false;
		}

		ModifiedEntityTypeEntity that = (ModifiedEntityTypeEntity) o;

		if ( entityClassName != null ?
				!entityClassName.equals( that.entityClassName ) :
				that.entityClassName != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return entityClassName != null ? entityClassName.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "CustomTrackingRevisionEntity(entityClassName = " + entityClassName + ")";
	}
}
