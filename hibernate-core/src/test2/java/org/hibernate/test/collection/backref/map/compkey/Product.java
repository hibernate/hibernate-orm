/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.backref.map.compkey;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Product implementation
 *
 * @author Steve Ebersole
 */
public class Product implements Serializable {
	private String name;
	private Map parts = new HashMap();

	public Product() {
	}

	public Product(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Map getParts() {
		return parts;
	}

	public void setParts(Map parts) {
		this.parts = parts;
	}
}
