/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany.ids;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.ids.EmbId;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefIngEmbIdEntity {
	@EmbeddedId
	private EmbId id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	private SetRefEdEmbIdEntity reference;

	public SetRefIngEmbIdEntity() {
	}

	public SetRefIngEmbIdEntity(EmbId id, String data, SetRefEdEmbIdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public SetRefIngEmbIdEntity(String data, SetRefEdEmbIdEntity reference) {
		this.data = data;
		this.reference = reference;
	}

	public EmbId getId() {
		return id;
	}

	public void setId(EmbId id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public SetRefEdEmbIdEntity getReference() {
		return reference;
	}

	public void setReference(SetRefEdEmbIdEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SetRefIngEmbIdEntity) ) {
			return false;
		}

		SetRefIngEmbIdEntity that = (SetRefIngEmbIdEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
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
		return "SetRefIngEmbIdEntity(id = " + id + ", data = " + data + ")";
	}
}
