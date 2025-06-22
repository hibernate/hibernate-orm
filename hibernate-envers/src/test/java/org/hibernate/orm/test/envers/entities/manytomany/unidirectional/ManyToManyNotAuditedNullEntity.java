/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.unidirectional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "M2M_N_AUD_NULL")
public class ManyToManyNotAuditedNullEntity implements Serializable {
	@Id
	private Integer id;

	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToMany(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinTable(joinColumns = @JoinColumn(name = "M2MNotAudited_id"))
	private List<UnversionedStrTestEntity> references = new ArrayList<UnversionedStrTestEntity>();

	protected ManyToManyNotAuditedNullEntity() {
	}

	public ManyToManyNotAuditedNullEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ManyToManyNotAuditedNullEntity ) ) return false;

		ManyToManyNotAuditedNullEntity that = (ManyToManyNotAuditedNullEntity) o;

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
		return "ManyToManyNotAuditedNullEntity(id = " + id + ", data = " + data + ")";
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
}
