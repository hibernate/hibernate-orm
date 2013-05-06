package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "ExplicitTransitiveChild")
@Audited(auditParents = {TransitiveParentEntity.class})
public class ExplicitTransitiveChildEntity extends TransitiveParentEntity {
	private String child;

	public ExplicitTransitiveChildEntity() {
		super( null, null, null, null );
	}

	public ExplicitTransitiveChildEntity(Long id, String grandparent, String notAudited, String parent, String child) {
		super( id, grandparent, notAudited, parent );
		this.child = child;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ExplicitTransitiveChildEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ExplicitTransitiveChildEntity that = (ExplicitTransitiveChildEntity) o;

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
		return "ExplicitTransitiveChildEntity(" + super.toString() + ", child = " + child + ")";
	}

	public String getChild() {
		return child;
	}

	public void setChild(String child) {
		this.child = child;
	}
}
