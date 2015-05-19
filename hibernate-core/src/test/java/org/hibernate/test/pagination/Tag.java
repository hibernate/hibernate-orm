/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.pagination;


public class Tag {
	private int id;
	private String surrogate;
	
	public Tag() {
	
	}

	public Tag(String surrogate) {
		this.surrogate = surrogate;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSurrogate() {
		return surrogate;
	}

	public void setSurrogate(String surrogate) {
		this.surrogate = surrogate;
	}

}
