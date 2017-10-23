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
public class D {
	private D_PK id;

	@EmbeddedId
	public D_PK getId() {
		return id;
	}

	public void setId(D_PK id) {
		this.id = id;
	}
}
