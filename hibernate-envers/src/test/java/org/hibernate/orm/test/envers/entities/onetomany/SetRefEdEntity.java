/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * ReferencEd entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefEdEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "reference", cascade = CascadeType.ALL)
	private Set<SetRefIngEntity> reffering;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(fetch = FetchType.LAZY)
	private SetRefIngEntity ref;

	public SetRefEdEntity() {
	}

	public SetRefEdEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public SetRefEdEntity(String data) {
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

	public Set<SetRefIngEntity> getReffering() {
		return reffering;
	}

	public void setReffering(Set<SetRefIngEntity> reffering) {
		this.reffering = reffering;
	}

	public SetRefIngEntity getRef() {
		return ref;
	}

	public void setRef(SetRefIngEntity ref) {
		this.ref = ref;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SetRefEdEntity) ) {
			return false;
		}

		SetRefEdEntity that = (SetRefEdEntity) o;

		if ( data != null ? !data.equals( that.getData() ) : that.getData() != null ) {
			return false;
		}
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
		return "SetRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}
