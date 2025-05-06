/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany.detached.ids;

import java.util.Set;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;

/**
 * Set collection of references entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "SetRefCollEmbId")
public class SetRefCollEntityEmbId {
	@EmbeddedId
	private EmbId id;

	@Audited
	private String data;

	@Audited
	@OneToMany
	private Set<EmbIdTestEntity> collection;

	public SetRefCollEntityEmbId() {
	}

	public SetRefCollEntityEmbId(EmbId id, String data) {
		this.id = id;
		this.data = data;
	}

	public SetRefCollEntityEmbId(String data) {
		this.data = data;
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

	public Set<EmbIdTestEntity> getCollection() {
		return collection;
	}

	public void setCollection(Set<EmbIdTestEntity> collection) {
		this.collection = collection;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SetRefCollEntityEmbId) ) {
			return false;
		}

		SetRefCollEntityEmbId that = (SetRefCollEntityEmbId) o;

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
		return "SetRefCollEntityEmbId(id = " + id + ", data = " + data + ")";
	}
}
