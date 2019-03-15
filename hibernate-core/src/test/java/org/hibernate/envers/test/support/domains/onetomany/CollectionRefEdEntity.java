/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * ReferencEd entity
 *
 * @author Adam Warski (adam at warski dot org)
 *
 * @see org.hibernate.envers.test.serialization.SerializingCollectionTest
 */
@Entity
public class CollectionRefEdEntity implements Serializable {
	private static final long serialVersionUID = -1694020123633796683L;

	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany(mappedBy = "reference")
	private Collection<CollectionRefIngEntity> reffering;

	public CollectionRefEdEntity() {
	}

	public CollectionRefEdEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public CollectionRefEdEntity(String data) {
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

	public Collection<CollectionRefIngEntity> getReffering() {
		return reffering;
	}

	public void setReffering(Collection<CollectionRefIngEntity> reffering) {
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
		CollectionRefEdEntity that = (CollectionRefEdEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "CollectionRefEdEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}