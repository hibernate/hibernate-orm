/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class CollectionRefIngEntity {
	private static final long serialVersionUID = -9019967223928425707L;

	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	private CollectionRefEdEntity reference;

	public CollectionRefIngEntity() {
	}

	public CollectionRefIngEntity(Integer id, String data, CollectionRefEdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public CollectionRefIngEntity(Integer id, String data) {
		this.id = id;
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

	public CollectionRefEdEntity getReference() {
		return reference;
	}

	public void setReference(CollectionRefEdEntity reference) {
		this.reference = reference;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		CollectionRefIngEntity that = (CollectionRefIngEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "CollectionRefIngEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}