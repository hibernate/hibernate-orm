/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.naming;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class DetachedNamingTestEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany
	@JoinTable(name = "UNI_NAMING_TEST",
			   joinColumns = @JoinColumn(name = "ID_1"),
			   inverseJoinColumns = @JoinColumn(name = "ID_2"))
	private Set<StrTestEntity> collection;

	public DetachedNamingTestEntity() {
	}

	public DetachedNamingTestEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public DetachedNamingTestEntity(String data) {
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

	public Set<StrTestEntity> getCollection() {
		return collection;
	}

	public void setCollection(Set<StrTestEntity> collection) {
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
		DetachedNamingTestEntity that = (DetachedNamingTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "DetachedNamingTestEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
