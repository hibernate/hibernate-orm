/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.cascade;


/**
 * todo: describe Other
 *
 * @author Steve Ebersole
 */
public class Other {
	private Long id;
	private Parent owner;

	public Long getId() {
		return id;
	}

	public Parent getOwner() {
		return owner;
	}

	public void setOwner(Parent owner) {
		this.owner = owner;
	}
}
