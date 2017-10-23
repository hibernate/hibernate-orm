/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class JoinNamingRefIngEntity {
	@Id
	@GeneratedValue
	@Column(name = "jnrie_id")
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	@JoinColumn(name = "jnree_column_reference")
	private JoinNamingRefEdEntity reference;

	public JoinNamingRefIngEntity() {
	}

	public JoinNamingRefIngEntity(Integer id, String data, JoinNamingRefEdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public JoinNamingRefIngEntity(String data, JoinNamingRefEdEntity reference) {
		this.data = data;
		this.reference = reference;
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

	public JoinNamingRefEdEntity getReference() {
		return reference;
	}

	public void setReference(JoinNamingRefEdEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JoinNamingRefIngEntity) ) {
			return false;
		}

		JoinNamingRefIngEntity that = (JoinNamingRefIngEntity) o;

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
		return "JoinNamingRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}