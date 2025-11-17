/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytoone.unidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "EM2O_N_AUD_NULL")
public class ExtManyToOneNotAuditedNullEntity extends ManyToOneNotAuditedNullEntity {
	@Audited
	private String extension = null;

	public ExtManyToOneNotAuditedNullEntity() {
	}

	public ExtManyToOneNotAuditedNullEntity(Integer id, String data, UnversionedStrTestEntity reference, String extension) {
		super( id, data, reference );
		this.extension = extension;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ExtManyToOneNotAuditedNullEntity ) ) return false;
		if ( !super.equals( o ) ) return false;

		ExtManyToOneNotAuditedNullEntity that = (ExtManyToOneNotAuditedNullEntity) o;

		if ( extension != null ? !extension.equals( that.extension ) : that.extension != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (extension != null ? extension.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ExtManyToOneNotAuditedNullEntity(" + super.toString() + ", extension = " + extension + ")";
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}
}
