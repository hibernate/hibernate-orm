/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Child Pk with many to one inside
 *
 * @author Emmanuel Bernard
 */
@Embeddable
public class ChildPk implements Serializable {
	public int nthChild;
	@ManyToOne
	@JoinColumn(name = "parentLastName", referencedColumnName = "p_lname", nullable = false)
	@JoinColumn(name = "parentFirstName", referencedColumnName = "firstName", nullable = false)
	public Parent parent;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ChildPk ) ) return false;

		final ChildPk childPk = (ChildPk) o;

		if ( nthChild != childPk.nthChild ) return false;
		if ( !parent.equals( childPk.parent ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = nthChild;
		result = 29 * result + parent.hashCode();
		return result;
	}
}
