/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.onetomany.detached;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.StrTestEntity;

/**
 * Set collection of references entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class DoubleSetRefCollEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany
	@JoinTable(name = "DOUBLE_STR_1")
	private Set<StrTestEntity> collection;

	@Audited
	@OneToMany
	@JoinTable(name = "DOUBLE_STR_2")
	private Set<StrTestEntity> collection2;

	public DoubleSetRefCollEntity() {
	}

	public DoubleSetRefCollEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public DoubleSetRefCollEntity(String data) {
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

	public Set<StrTestEntity> getCollection2() {
		return collection2;
	}

	public void setCollection2(Set<StrTestEntity> collection2) {
		this.collection2 = collection2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DoubleSetRefCollEntity) ) {
			return false;
		}

		DoubleSetRefCollEntity that = (DoubleSetRefCollEntity) o;

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
		return "DoubleSetRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}