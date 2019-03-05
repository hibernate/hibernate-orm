/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany.detached;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.onetomany.detached.DoubleJoinColumnBidirectionalListTest} test.
 * Owning side of the relations.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "DoubleJoinColBiRefIng")
@Audited
public class DoubleListJoinColumnBidirectionalRefIngEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@OneToMany
	@JoinColumn(name = "some_join_column_1")
	@AuditMappedBy(mappedBy = "owner")
	private List<DoubleListJoinColumnBidirectionalRefEdEntity1> references1 = new ArrayList<>();

	@OneToMany
	@JoinColumn(name = "some_join_column_2")
	@AuditMappedBy(mappedBy = "owner")
	private List<DoubleListJoinColumnBidirectionalRefEdEntity2> references2 = new ArrayList<>();

	public DoubleListJoinColumnBidirectionalRefIngEntity() {
	}

	public DoubleListJoinColumnBidirectionalRefIngEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public DoubleListJoinColumnBidirectionalRefIngEntity(String data) {
		this( null, data );
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

	public List<DoubleListJoinColumnBidirectionalRefEdEntity1> getReferences1() {
		return references1;
	}

	public void setReferences1(List<DoubleListJoinColumnBidirectionalRefEdEntity1> references1) {
		this.references1 = references1;
	}

	public List<DoubleListJoinColumnBidirectionalRefEdEntity2> getReferences2() {
		return references2;
	}

	public void setReferences2(List<DoubleListJoinColumnBidirectionalRefEdEntity2> references2) {
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
		DoubleListJoinColumnBidirectionalRefIngEntity that = (DoubleListJoinColumnBidirectionalRefIngEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "DoubleListJoinColumnBidirectionalRefIngEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}