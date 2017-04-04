/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.fkcircularity;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * Test entities ANN-722.
 * 
 * @author Hardy Ferentschik
 *
 */
@Entity
public class A {
	private A_PK id;

	@EmbeddedId
	public A_PK getId() {
		return id;
	}

	public void setId(A_PK id) {
		this.id = id;
	}
}
