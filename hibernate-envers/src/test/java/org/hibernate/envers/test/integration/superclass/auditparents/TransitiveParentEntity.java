/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
@Audited(auditParents = {MappedGrandparentEntity.class})
public class TransitiveParentEntity extends MappedGrandparentEntity {
	private String parent;

	public TransitiveParentEntity() {
		super( null, null, null );
	}

	public TransitiveParentEntity(Long id, String grandparent, String notAudited, String parent) {
		super( id, grandparent, notAudited );
		this.parent = parent;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof TransitiveParentEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		TransitiveParentEntity that = (TransitiveParentEntity) o;

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
		return "TransitiveParentEntity(" + super.toString() + ", parent = " + parent + ")";
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}
}
