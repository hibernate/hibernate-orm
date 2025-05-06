/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

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
