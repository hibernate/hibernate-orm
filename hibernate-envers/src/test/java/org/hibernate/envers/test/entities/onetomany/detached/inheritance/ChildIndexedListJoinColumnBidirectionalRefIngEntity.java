package org.hibernate.envers.test.entities.onetomany.detached.inheritance;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.integration.onetomany.detached.InheritanceIndexedJoinColumnBidirectionalList} test.
 * Child, owning side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ChildIdxJoinColBiRefIng")
@Audited
public class ChildIndexedListJoinColumnBidirectionalRefIngEntity
		extends ParentIndexedListJoinColumnBidirectionalRefIngEntity {
	private String data2;

	public ChildIndexedListJoinColumnBidirectionalRefIngEntity() {
	}

	public ChildIndexedListJoinColumnBidirectionalRefIngEntity(
			Integer id,
			String data,
			String data2,
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity... references) {
		super( id, data, references );
		this.data2 = data2;
	}

	public ChildIndexedListJoinColumnBidirectionalRefIngEntity(
			String data,
			String data2,
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity... references) {
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
		if ( !(o instanceof ChildIndexedListJoinColumnBidirectionalRefIngEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildIndexedListJoinColumnBidirectionalRefIngEntity that = (ChildIndexedListJoinColumnBidirectionalRefIngEntity) o;

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

	public String toString() {
		return "ChildIndexedListJoinColumnBidirectionalRefIngEntity(id = " + getId() + ", data = " + getData() + ", data2 = " + data2 + ")";
	}
}
