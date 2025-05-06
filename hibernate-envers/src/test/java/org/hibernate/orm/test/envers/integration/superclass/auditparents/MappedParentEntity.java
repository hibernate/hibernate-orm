/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditparents;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.orm.test.envers.entities.StrIntTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class MappedParentEntity extends MappedGrandparentEntity {
	private String parent;

	@ManyToOne
	private StrIntTestEntity relation;

	public MappedParentEntity(
			Long id,
			String grandparent,
			String notAudited,
			String parent,
			StrIntTestEntity relation) {
		super( id, grandparent, notAudited );
		this.parent = parent;
		this.relation = relation;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MappedParentEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		MappedParentEntity that = (MappedParentEntity) o;

		if ( parent != null ? !parent.equals( that.parent ) : that.parent != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (parent != null ? parent.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "MappedParentEntity(" + super.toString() + ", parent = " + parent + ", relation = " + relation + ")";
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public StrIntTestEntity getRelation() {
		return relation;
	}

	public void setRelation(StrIntTestEntity relation) {
		this.relation = relation;
	}
}
