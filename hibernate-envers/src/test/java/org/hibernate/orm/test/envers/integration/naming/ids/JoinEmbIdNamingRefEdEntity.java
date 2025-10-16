/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming.ids;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * ReferencEd entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "JoinEmbIdRefEd")
public class JoinEmbIdNamingRefEdEntity {
	@Id
	private EmbIdNaming id;

	@Audited
	private String data;

	@Audited
	@OneToMany(mappedBy = "reference")
	private List<JoinEmbIdNamingRefIngEntity> reffering;

	public JoinEmbIdNamingRefEdEntity() {
	}

	public JoinEmbIdNamingRefEdEntity(EmbIdNaming id, String data) {
		this.id = id;
		this.data = data;
	}

	public JoinEmbIdNamingRefEdEntity(String data) {
		this.data = data;
	}

	public EmbIdNaming getId() {
		return id;
	}

	public void setId(EmbIdNaming id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public List<JoinEmbIdNamingRefIngEntity> getReffering() {
		return reffering;
	}

	public void setReffering(List<JoinEmbIdNamingRefIngEntity> reffering) {
		this.reffering = reffering;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JoinEmbIdNamingRefEdEntity) ) {
			return false;
		}

		JoinEmbIdNamingRefEdEntity that = (JoinEmbIdNamingRefEdEntity) o;

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
		return "JoinEmbIdNamingRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}
