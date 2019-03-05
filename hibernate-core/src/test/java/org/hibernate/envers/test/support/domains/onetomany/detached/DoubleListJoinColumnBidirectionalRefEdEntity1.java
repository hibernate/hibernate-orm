/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany.detached;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.onetomany.detached.DoubleJoinColumnBidirectionalListTest} test.
 * Owned side of the first relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "DoubleJoinColBiRefEd1")
@Audited
public class DoubleListJoinColumnBidirectionalRefEdEntity1 {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@ManyToOne
	@JoinColumn(name = "some_join_column_1", insertable = false, updatable = false)
	private DoubleListJoinColumnBidirectionalRefIngEntity owner;

	public DoubleListJoinColumnBidirectionalRefEdEntity1() {
	}

	public DoubleListJoinColumnBidirectionalRefEdEntity1(
			Integer id,
			String data,
			DoubleListJoinColumnBidirectionalRefIngEntity owner) {
		this.id = id;
		this.data = data;
		this.owner = owner;
	}

	public DoubleListJoinColumnBidirectionalRefEdEntity1(
			String data,
			DoubleListJoinColumnBidirectionalRefIngEntity owner) {
		this.data = data;
		this.owner = owner;
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

	public DoubleListJoinColumnBidirectionalRefIngEntity getOwner() {
		return owner;
	}

	public void setOwner(DoubleListJoinColumnBidirectionalRefIngEntity owner) {
		this.owner = owner;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DoubleListJoinColumnBidirectionalRefEdEntity1 that = (DoubleListJoinColumnBidirectionalRefEdEntity1) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "DoubleListJoinColumnBidirectionalRefEdEntity1{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}