/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.Entity;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.StrIntTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited(auditParents = {MappedParentEntity.class, MappedGrandparentEntity.class})
public class ChildMultipleParentsEntity extends MappedParentEntity {
	private String child;

	public ChildMultipleParentsEntity() {
		super( null, null, null, null, null );
	}

	public ChildMultipleParentsEntity(
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
		if ( !(o instanceof ChildMultipleParentsEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildMultipleParentsEntity that = (ChildMultipleParentsEntity) o;

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
		return "ChildMultipleParentsEntity(" + super.toString() + ", child = " + child + ")";
	}

	public String getChild() {
		return child;
	}

	public void setChild(String child) {
		this.child = child;
	}
}

