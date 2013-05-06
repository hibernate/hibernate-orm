package org.hibernate.envers.test.entities.onetomany.detached;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.integration.onetomany.detached.DoubleJoinColumnBidirectionalList} test.
 * Owned side of the second relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "DoubleJoinColBiRefEd2")
@Audited
public class DoubleListJoinColumnBidirectionalRefEdEntity2 {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@ManyToOne
	@JoinColumn(name = "some_join_column_2", insertable = false, updatable = false)
	private DoubleListJoinColumnBidirectionalRefIngEntity owner;

	public DoubleListJoinColumnBidirectionalRefEdEntity2() {
	}

	public DoubleListJoinColumnBidirectionalRefEdEntity2(
			Integer id,
			String data,
			DoubleListJoinColumnBidirectionalRefIngEntity owner) {
		this.id = id;
		this.data = data;
		this.owner = owner;
	}

	public DoubleListJoinColumnBidirectionalRefEdEntity2(
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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DoubleListJoinColumnBidirectionalRefEdEntity2) ) {
			return false;
		}

		DoubleListJoinColumnBidirectionalRefEdEntity2 that = (DoubleListJoinColumnBidirectionalRefEdEntity2) o;

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
		return "DoubleListJoinColumnBidirectionalRefIngEntity2(id = " + id + ", data = " + data + ")";
	}
}