/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.onetomany.detached;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.envers.test.integration.onetomany.detached.JoinColumnBidirectionalListWithInheritance} test.
 * Owned parent side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ListJoinColBiInhRefEdPar")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue("1")
@Audited
public class ListJoinColumnBidirectionalInheritanceRefEdParentEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String parentData;

	@ManyToOne
	@JoinColumn(name = "some_join_column", insertable = false, updatable = false)
	private ListJoinColumnBidirectionalInheritanceRefIngEntity owner;

	public ListJoinColumnBidirectionalInheritanceRefEdParentEntity() {
	}

	public ListJoinColumnBidirectionalInheritanceRefEdParentEntity(
			Integer id,
			String parentData,
			ListJoinColumnBidirectionalInheritanceRefIngEntity owner) {
		this.id = id;
		this.parentData = parentData;
		this.owner = owner;
	}

	public ListJoinColumnBidirectionalInheritanceRefEdParentEntity(
			String parentData,
			ListJoinColumnBidirectionalInheritanceRefIngEntity owner) {
		this.parentData = parentData;
		this.owner = owner;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ListJoinColumnBidirectionalInheritanceRefIngEntity getOwner() {
		return owner;
	}

	public void setOwner(ListJoinColumnBidirectionalInheritanceRefIngEntity owner) {
		this.owner = owner;
	}

	public String getParentData() {
		return parentData;
	}

	public void setParentData(String parentData) {
		this.parentData = parentData;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListJoinColumnBidirectionalInheritanceRefEdParentEntity) ) {
			return false;
		}

		ListJoinColumnBidirectionalInheritanceRefEdParentEntity that = (ListJoinColumnBidirectionalInheritanceRefEdParentEntity) o;

		if ( parentData != null ? !parentData.equals( that.parentData ) : that.parentData != null ) {
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
		result = 31 * result + (parentData != null ? parentData.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ListJoinColumnBidirectionalInheritanceRefEdParentEntity(id = " + id + ", parentData = " + parentData + ")";
	}
}