/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$

package org.hibernate.test.annotations.derivedidentities;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class DependentId implements Serializable {
	String name;

	long empPK;	// corresponds to PK type of Employee

	public DependentId() {
	}

	public DependentId(long empPK, String name) {
		this.empPK = empPK;
		this.name = name;
	}

	public String getName() {
		return name;
	}
}


