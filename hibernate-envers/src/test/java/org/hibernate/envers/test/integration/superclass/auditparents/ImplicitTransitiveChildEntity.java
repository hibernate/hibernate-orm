/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "ImplicitTransitiveChild")
@Audited
public class ImplicitTransitiveChildEntity extends TransitiveParentEntity {
	private String child;

	public ImplicitTransitiveChildEntity() {
		super( null, null, null, null );
	}

	public ImplicitTransitiveChildEntity(Long id, String grandparent, String notAudited, String parent, String child) {
		super( id, grandparent, notAudited, parent );
		this.child = child;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ImplicitTransitiveChildEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ImplicitTransitiveChildEntity that = (ImplicitTransitiveChildEntity) o;

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
		return "ImplicitTransitiveChildEntity(" + super.toString() + ", child = " + child + ")";
	}

	public String getChild() {
		return child;
	}

	public void setChild(String child) {
		this.child = child;
	}
}
