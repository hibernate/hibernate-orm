/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany.detached;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.orm.test.envers.integration.onetomany.detached.JoinColumnBidirectionalList} test.
 * Owning side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ListJoinColBiRefIng")
@Audited
public class ListJoinColumnBidirectionalRefIngEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@OneToMany
	@JoinColumn(name = "some_join_column")
	@AuditMappedBy(mappedBy = "owner")
	private List<ListJoinColumnBidirectionalRefEdEntity> references;

	public ListJoinColumnBidirectionalRefIngEntity() {
	}

	public ListJoinColumnBidirectionalRefIngEntity(
			Integer id,
			String data,
			ListJoinColumnBidirectionalRefEdEntity... references) {
		this.id = id;
		this.data = data;
		this.references = new ArrayList<ListJoinColumnBidirectionalRefEdEntity>();
		this.references.addAll( Arrays.asList( references ) );
	}

	public ListJoinColumnBidirectionalRefIngEntity(String data, ListJoinColumnBidirectionalRefEdEntity... references) {
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

	public List<ListJoinColumnBidirectionalRefEdEntity> getReferences() {
		return references;
	}

	public void setReferences(List<ListJoinColumnBidirectionalRefEdEntity> references) {
		this.references = references;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListJoinColumnBidirectionalRefIngEntity) ) {
			return false;
		}

		ListJoinColumnBidirectionalRefIngEntity that = (ListJoinColumnBidirectionalRefIngEntity) o;

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
		return "ListJoinColumnBidirectionalRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}
