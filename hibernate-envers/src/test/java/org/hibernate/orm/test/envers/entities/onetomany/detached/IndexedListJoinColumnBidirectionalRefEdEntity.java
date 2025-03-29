/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany.detached;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.orm.test.envers.integration.onetomany.detached.IndexedJoinColumnBidirectionalList} test.
 * Owned side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "IdxListJoinColBiRefEd")
@Audited
public class IndexedListJoinColumnBidirectionalRefEdEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@Column(name = "indexed_index", insertable = false, updatable = false)
	private Integer position;

	@ManyToOne
	@JoinColumn(name = "indexed_join_column", insertable = false, updatable = false)
	private IndexedListJoinColumnBidirectionalRefIngEntity owner;

	public IndexedListJoinColumnBidirectionalRefEdEntity() {
	}

	public IndexedListJoinColumnBidirectionalRefEdEntity(
			Integer id,
			String data,
			IndexedListJoinColumnBidirectionalRefIngEntity owner) {
		this.id = id;
		this.data = data;
		this.owner = owner;
	}

	public IndexedListJoinColumnBidirectionalRefEdEntity(
			String data,
			IndexedListJoinColumnBidirectionalRefIngEntity owner) {
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

	public IndexedListJoinColumnBidirectionalRefIngEntity getOwner() {
		return owner;
	}

	public void setOwner(IndexedListJoinColumnBidirectionalRefIngEntity owner) {
		this.owner = owner;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IndexedListJoinColumnBidirectionalRefEdEntity) ) {
			return false;
		}

		IndexedListJoinColumnBidirectionalRefEdEntity that = (IndexedListJoinColumnBidirectionalRefEdEntity) o;

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
		return "IndexedListJoinColumnBidirectionalRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}
