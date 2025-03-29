/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import jakarta.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
@Audited(auditParents = {MappedGrandparentEntity.class})
public class ChildCompleteEntity extends MappedParentEntity {
	private String child;

	public ChildCompleteEntity() {
		super( null, null, null, null, null );
	}

	public ChildCompleteEntity(
			Long id,
			String grandparent,
			String notAudited,
			String parent,
			String child,
			StrIntTestEntity relation) {
		super( id, grandparent, notAudited, parent, relation );
		this.child = child;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ChildCompleteEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildCompleteEntity that = (ChildCompleteEntity) o;

		if ( child != null ? !child.equals( that.child ) : that.child != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (child != null ? child.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ChildCompleteEntity(" + super.toString() + ", child = " + child + ")";
	}

	public String getChild() {
		return child;
	}

	public void setChild(String child) {
		this.child = child;
	}
}
