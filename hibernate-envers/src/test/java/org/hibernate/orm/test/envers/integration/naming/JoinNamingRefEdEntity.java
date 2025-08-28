/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * ReferencEd entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class JoinNamingRefEdEntity {
	@Id
	@GeneratedValue
	@Column(name = "jnree_id")
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany(mappedBy = "reference")
	private List<JoinNamingRefIngEntity> reffering;

	public JoinNamingRefEdEntity() {
	}

	public JoinNamingRefEdEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public JoinNamingRefEdEntity(String data) {
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

	public List<JoinNamingRefIngEntity> getReffering() {
		return reffering;
	}

	public void setReffering(List<JoinNamingRefIngEntity> reffering) {
		this.reffering = reffering;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JoinNamingRefEdEntity) ) {
			return false;
		}

		JoinNamingRefEdEntity that = (JoinNamingRefEdEntity) o;

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
		return "JoinNamingRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}
