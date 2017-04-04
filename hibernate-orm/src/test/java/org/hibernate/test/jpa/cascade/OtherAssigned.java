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
public class OtherAssigned {
	private Long id;
	private ParentAssigned owner;

	public OtherAssigned() {
	}

	public OtherAssigned(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public ParentAssigned getOwner() {
		return owner;
	}

	public void setOwner(ParentAssigned owner) {
		this.owner = owner;
	}
}
