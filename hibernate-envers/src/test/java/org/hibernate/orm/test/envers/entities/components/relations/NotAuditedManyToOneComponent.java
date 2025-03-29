/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components.relations;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.envers.NotAudited;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
@Table(name = "NotAudM2OCompEmb")
public class NotAuditedManyToOneComponent {
	@ManyToOne
	@NotAudited
	private UnversionedStrTestEntity entity;

	private String data;

	public NotAuditedManyToOneComponent(UnversionedStrTestEntity entity, String data) {
		this.entity = entity;
		this.data = data;
	}

	public NotAuditedManyToOneComponent() {
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public UnversionedStrTestEntity getEntity() {
		return entity;
	}

	public void setEntity(UnversionedStrTestEntity entity) {
		this.entity = entity;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NotAuditedManyToOneComponent that = (NotAuditedManyToOneComponent) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( entity != null ? !entity.equals( that.entity ) : that.entity != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = entity != null ? entity.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "NotAuditedManyToOneComponent(str1 = " + data + ")";
	}
}
