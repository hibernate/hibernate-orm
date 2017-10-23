/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.extralazy;

import java.util.HashMap;
import java.util.Map;



public class Parent {

	private String id;
	
	private Map <String, Child> children = new HashMap<String, Child>  ();

	public void setChildren(Map <String, Child> children) {
		this.children = children;
	}

	public Map <String, Child> getChildren() {
		return children;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}	
}
