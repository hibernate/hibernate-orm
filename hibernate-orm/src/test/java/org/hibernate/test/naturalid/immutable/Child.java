/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.immutable;


/**
 * @author Alex Burgel
 */
public class Child {
	
	private Long id;
    private Parent parent;
	private String name;
	
	Child() {}

	public Child(String name, Parent parent) {
		this.name = name;
        this.parent = parent;
	}
	
}
