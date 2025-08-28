/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming.ids;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "JoinEmbIdRefIng")
public class JoinEmbIdNamingRefIngEntity {
	@Id
	private EmbIdNaming id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	@JoinColumns({
						@JoinColumn(name = "XX_reference", referencedColumnName = "XX"),
						@JoinColumn(name = "YY_reference", referencedColumnName = "YY")
				})
	private JoinEmbIdNamingRefEdEntity reference;

	public JoinEmbIdNamingRefIngEntity() {
	}

	public JoinEmbIdNamingRefIngEntity(EmbIdNaming id, String data, JoinEmbIdNamingRefEdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public JoinEmbIdNamingRefIngEntity(String data, JoinEmbIdNamingRefEdEntity reference) {
		this.data = data;
		this.reference = reference;
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

	public JoinEmbIdNamingRefEdEntity getReference() {
		return reference;
	}

	public void setReference(JoinEmbIdNamingRefEdEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JoinEmbIdNamingRefIngEntity) ) {
			return false;
		}

		JoinEmbIdNamingRefIngEntity that = (JoinEmbIdNamingRefIngEntity) o;

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
		return "JoinEmbIdNamingRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}
