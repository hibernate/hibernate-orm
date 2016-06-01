/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ondelete.toone.hbm;

/**
 * @author Vlad Mihalcea
 */
public class GrandChild {

	private Long id;

	private Child parent;

	public Child getParent() {
		return parent;
	}

	public void setParent(Child parent) {
		this.parent = parent;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
