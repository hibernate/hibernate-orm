/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany.detached.inheritance;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.onetomany.detached.InheritanceIndexedJoinColumnBidirectionalListTest}.
 * Child, owning side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ChildIdxJoinColBiRefIng")
@Audited
public class ChildIndexedEntity
		extends ParentIndexedEntity {
	private String data2;

	public ChildIndexedEntity() {
	}

	public ChildIndexedEntity(
			Integer id,
			String data,
			String data2,
			ParentOwnedIndexedEntity... references) {
		super( id, data, references );
		this.data2 = data2;
	}

	public ChildIndexedEntity(
			String data,
			String data2,
			ParentOwnedIndexedEntity... references) {
		super( data, references );
		this.data2 = data2;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ChildIndexedEntity ) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildIndexedEntity that = (ChildIndexedEntity) o;

		if ( data2 != null ? !data2.equals( that.data2 ) : that.data2 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (data2 != null ? data2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ChildIndexedEntity(id = " + getId() + ", data = " + getData() + ", data2 = " + data2 + ")";
	}
}
