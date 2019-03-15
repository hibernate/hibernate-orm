/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytomany;

import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.IntNoAutoIdTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class WhereJoinTableEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@ManyToMany
	@JoinTable(
			name = "wjte_ite_join",
			joinColumns = @JoinColumn(name = "wjte_id"),
			inverseJoinColumns = @JoinColumn(name = "ite_id")
	)
	@WhereJoinTable(clause = "ite_id < 20")
	private List<IntNoAutoIdTestEntity> references1;

	@ManyToMany
	@JoinTable(
			name = "wjte_ite_join",
			joinColumns = @JoinColumn(name = "wjte_id"),
			inverseJoinColumns = @JoinColumn(name = "ite_id")
	)
	@WhereJoinTable(clause = "ite_id >= 20")
	private List<IntNoAutoIdTestEntity> references2;

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

	public List<IntNoAutoIdTestEntity> getReferences1() {
		return references1;
	}

	public void setReferences1(List<IntNoAutoIdTestEntity> references1) {
		this.references1 = references1;
	}

	public List<IntNoAutoIdTestEntity> getReferences2() {
		return references2;
	}

	public void setReferences2(List<IntNoAutoIdTestEntity> references2) {
		this.references2 = references2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		WhereJoinTableEntity that = (WhereJoinTableEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "WhereJoinTableEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
