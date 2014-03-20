package org.hibernate.envers.test.entities.onetomany.detached.inheritance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.integration.onetomany.detached.InheritanceIndexedJoinColumnBidirectionalList} test.
 * Parent, owning side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
@Table(name = "ParentIdxJoinColBiRefIng")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ParentIndexedListJoinColumnBidirectionalRefIngEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@OneToMany
	@JoinColumn(name = "indexed_join_column")
	@OrderColumn(name = "indexed_index")
	@AuditMappedBy(mappedBy = "owner", positionMappedBy = "position")
	private List<ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity> references;

	public ParentIndexedListJoinColumnBidirectionalRefIngEntity() {
	}

	public ParentIndexedListJoinColumnBidirectionalRefIngEntity(
			Integer id,
			String data,
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity... references) {
		this.id = id;
		this.data = data;
		this.references = new ArrayList<ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity>();
		this.references.addAll( Arrays.asList( references ) );
	}

	public ParentIndexedListJoinColumnBidirectionalRefIngEntity(
			String data,
			ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity... references) {
		this( null, data, references );
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

	public List<ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity> getReferences() {
		return references;
	}

	public void setReferences(List<ParentOwnedIndexedListJoinColumnBidirectionalRefEdEntity> references) {
		this.references = references;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ParentIndexedListJoinColumnBidirectionalRefIngEntity) ) {
			return false;
		}

		ParentIndexedListJoinColumnBidirectionalRefIngEntity that = (ParentIndexedListJoinColumnBidirectionalRefIngEntity) o;

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
		return "ParentIndexedListJoinColumnBidirectionalRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}