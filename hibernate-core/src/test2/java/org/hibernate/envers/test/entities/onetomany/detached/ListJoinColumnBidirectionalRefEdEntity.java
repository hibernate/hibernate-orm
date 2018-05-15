/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.onetomany.detached;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.integration.onetomany.detached.JoinColumnBidirectionalList} test.
 * Owned side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ListJoinColBiRefEd")
@Audited
public class ListJoinColumnBidirectionalRefEdEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@ManyToOne
	@JoinColumn(name = "some_join_column", insertable = false, updatable = false)
	private ListJoinColumnBidirectionalRefIngEntity owner;

	public ListJoinColumnBidirectionalRefEdEntity() {
	}

	public ListJoinColumnBidirectionalRefEdEntity(
			Integer id,
			String data,
			ListJoinColumnBidirectionalRefIngEntity owner) {
		this.id = id;
		this.data = data;
		this.owner = owner;
	}

	public ListJoinColumnBidirectionalRefEdEntity(String data, ListJoinColumnBidirectionalRefIngEntity owner) {
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

	public ListJoinColumnBidirectionalRefIngEntity getOwner() {
		return owner;
	}

	public void setOwner(ListJoinColumnBidirectionalRefIngEntity owner) {
		this.owner = owner;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListJoinColumnBidirectionalRefEdEntity) ) {
			return false;
		}

		ListJoinColumnBidirectionalRefEdEntity that = (ListJoinColumnBidirectionalRefEdEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
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
		return "ListJoinColumnBidirectionalRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}
