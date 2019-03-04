/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany.detached;

import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

/**
 * List collection of references entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class ListRefCollEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany
	private List<StrTestEntity> collection;

	public ListRefCollEntity() {
	}

	public ListRefCollEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public ListRefCollEntity(String data) {
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

	public List<StrTestEntity> getCollection() {
		return collection;
	}

	public void setCollection(List<StrTestEntity> collection) {
		this.collection = collection;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ListRefCollEntity that = (ListRefCollEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "ListRefCollEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}