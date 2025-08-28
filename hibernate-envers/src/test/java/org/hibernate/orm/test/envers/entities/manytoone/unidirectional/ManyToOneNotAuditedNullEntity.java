/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytoone.unidirectional;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Audited
@Entity
@Table(name = "M2O_N_AUD_NULL")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ManyToOneNotAuditedNullEntity implements Serializable {
	@Id
	private Integer id;

	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@NotFound(action = NotFoundAction.IGNORE)
	private UnversionedStrTestEntity reference;

	protected ManyToOneNotAuditedNullEntity() {
	}

	public ManyToOneNotAuditedNullEntity(Integer id, String data, UnversionedStrTestEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ManyToOneNotAuditedNullEntity ) ) return false;

		ManyToOneNotAuditedNullEntity that = (ManyToOneNotAuditedNullEntity) o;

		if ( data != null ? !data.equals( that.getData() ) : that.getData() != null ) return false;
		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) return false;

		return true;
	}

	public int hashCode() {
		int result = ( id != null ? id.hashCode() : 0 );
		result = 31 * result + ( data != null ? data.hashCode() : 0 );
		return result;
	}

	public String toString() {
		return "ManyToOneNotAuditedNullEntity(id = " + id + ", data = " + data + ")";
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public UnversionedStrTestEntity getReference() {
		return reference;
	}

	public void setReference(UnversionedStrTestEntity reference) {
		this.reference = reference;
	}
}
