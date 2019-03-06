/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.naming;

import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JoinNamingRefEdEntity that = (JoinNamingRefEdEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "JoinNamingRefEdEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}