/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Category.java 6957 2005-05-31 04:21:58Z oneovthafew $
package org.hibernate.test.joinfetch;


/**
 * @author Gavin King
 */
public class Category {
	
	private String name;

	Category() {}

	public Category(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
