/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.mapcompelem;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gavin King
 */
public class Product {
	private String name;
	private Map parts = new HashMap();
	Product() {}
	public Product(String n) {
		name = n;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map getParts() {
		return parts;
	}
	public void setParts(Map users) {
		this.parts = users;
	}
}
