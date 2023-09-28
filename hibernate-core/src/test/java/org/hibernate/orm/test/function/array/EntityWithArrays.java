/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.function.array;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
@Entity
public class EntityWithArrays {

	@Id
	private Long id;

	@Column(name = "the_array")
	private String[] theArray;

	public EntityWithArrays() {
	}

	public EntityWithArrays(Long id, String[] theArray) {
		this.id = id;
		this.theArray = theArray;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String[] getTheArray() {
		return theArray;
	}

	public void setTheArray(String[] theArray) {
		this.theArray = theArray;
	}
}
