/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany.detached;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Entity for {@link org.hibernate.orm.test.envers.integration.onetomany.detached.IndexedJoinColumnBidirectionalList} test.
 * Owning side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "IdxListJoinColBiRefIng")
@Audited
public class IndexedListJoinColumnBidirectionalRefIngEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@OneToMany
	@JoinColumn(name = "indexed_join_column")
	@OrderColumn(name = "indexed_index")
	@AuditMappedBy(mappedBy = "owner", positionMappedBy = "position")
	private List<IndexedListJoinColumnBidirectionalRefEdEntity> references;

	public IndexedListJoinColumnBidirectionalRefIngEntity() {
	}

	public IndexedListJoinColumnBidirectionalRefIngEntity(
			Integer id,
			String data,
			IndexedListJoinColumnBidirectionalRefEdEntity... references) {
		this.id = id;
		this.data = data;
		this.references = new ArrayList<IndexedListJoinColumnBidirectionalRefEdEntity>();
		this.references.addAll( Arrays.asList( references ) );
	}

	public IndexedListJoinColumnBidirectionalRefIngEntity(
			String data,
			IndexedListJoinColumnBidirectionalRefEdEntity... references) {
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

	public List<IndexedListJoinColumnBidirectionalRefEdEntity> getReferences() {
		return references;
	}

	public void setReferences(List<IndexedListJoinColumnBidirectionalRefEdEntity> references) {
		this.references = references;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IndexedListJoinColumnBidirectionalRefIngEntity) ) {
			return false;
		}

		IndexedListJoinColumnBidirectionalRefIngEntity that = (IndexedListJoinColumnBidirectionalRefIngEntity) o;

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
		return "IndexedListJoinColumnBidirectionalRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}
