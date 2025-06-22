/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.unidirectional;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;

/**
 * @author Vladimir Klyushnikov
 */
@Entity
@Table(name = "M2M_IDX_LIST")
public class M2MIndexedListTargetNotAuditedEntity {

	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OrderColumn(name = "sortOrder")
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(joinColumns = @JoinColumn(name = "M2MIndexedList_id"))
	private List<UnversionedStrTestEntity> references = new ArrayList<UnversionedStrTestEntity>();


	public M2MIndexedListTargetNotAuditedEntity() {
	}


	public M2MIndexedListTargetNotAuditedEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public M2MIndexedListTargetNotAuditedEntity(Integer id, String data, List<UnversionedStrTestEntity> references) {
		this.id = id;
		this.data = data;
		this.references = references;
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


	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		M2MIndexedListTargetNotAuditedEntity that = (M2MIndexedListTargetNotAuditedEntity) o;

		//noinspection RedundantIfStatement
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return data != null ? data.hashCode() : 0;
	}


	@Override
	public String toString() {
		return "M2MIndexedListTargetNotAuditedEntity(id = " + id + ", data = " + data + ")";
	}
}
