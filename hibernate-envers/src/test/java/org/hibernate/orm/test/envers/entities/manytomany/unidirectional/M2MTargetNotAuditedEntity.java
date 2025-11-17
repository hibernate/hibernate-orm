/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.unidirectional;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;

/**
 * Audited entity with a many-to-many-reference to not audited entity.
 *
 * @author Toamsz Bech
 * @author Adam Warski
 */
@Entity
@Table(name = "M2MTargetNotAud")
public class M2MTargetNotAuditedEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToMany(fetch = FetchType.LAZY)
	private List<UnversionedStrTestEntity> references;

	public M2MTargetNotAuditedEntity() {
	}

	public M2MTargetNotAuditedEntity(Integer id, String data, List<UnversionedStrTestEntity> references) {
		this.id = id;
		this.data = data;
		this.references = references;
	}

	public M2MTargetNotAuditedEntity(String data, List<UnversionedStrTestEntity> references) {
		this.data = data;
		this.references = references;
	}

	public M2MTargetNotAuditedEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
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

	public List<UnversionedStrTestEntity> getReferences() {
		return references;
	}

	public void setReferences(List<UnversionedStrTestEntity> references) {
		this.references = references;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof M2MTargetNotAuditedEntity) ) {
			return false;
		}

		M2MTargetNotAuditedEntity that = (M2MTargetNotAuditedEntity) o;

		if ( data != null ? !data.equals( that.getData() ) : that.getData() != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "M2MTargetNotAuditedEntity(id = " + id + ", data = " + data + ")";
	}
}
