/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany.detached;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * Entity for {@link org.hibernate.orm.test.envers.integration.onetomany.detached.JoinColumnBidirectionalListWithInheritance} test.
 * Owned child side of the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@DiscriminatorValue("2")
@Audited
public class ListJoinColumnBidirectionalInheritanceRefEdChildEntity
		extends ListJoinColumnBidirectionalInheritanceRefEdParentEntity {
	private String childData;

	public ListJoinColumnBidirectionalInheritanceRefEdChildEntity() {
	}

	public ListJoinColumnBidirectionalInheritanceRefEdChildEntity(
			Integer id,
			String parentData,
			ListJoinColumnBidirectionalInheritanceRefIngEntity owner,
			String childData) {
		super( id, parentData, owner );
		this.childData = childData;
	}

	public ListJoinColumnBidirectionalInheritanceRefEdChildEntity(
			String parentData,
			ListJoinColumnBidirectionalInheritanceRefIngEntity owner,
			String childData) {
		super( parentData, owner );
		this.childData = childData;
	}

	public String getChildData() {
		return childData;
	}

	public void setChildData(String childData) {
		this.childData = childData;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ListJoinColumnBidirectionalInheritanceRefEdChildEntity that = (ListJoinColumnBidirectionalInheritanceRefEdChildEntity) o;

		//noinspection RedundantIfStatement
		if ( childData != null ? !childData.equals( that.childData ) : that.childData != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (childData != null ? childData.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ListJoinColumnBidirectionalInheritanceRefEdChildEntity(id = " + getId() +
				", parentData = " + getParentData() + ", childData = " + childData + ")";
	}
}
