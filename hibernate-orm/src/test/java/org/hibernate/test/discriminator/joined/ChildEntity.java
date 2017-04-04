/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.discriminator.joined;

/**
 * @author Chris Cranford
 */
public class ChildEntity extends ParentEntity {
	private String name;

	ChildEntity() {

	}

	ChildEntity(Integer id, String name) {
		super( id );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
