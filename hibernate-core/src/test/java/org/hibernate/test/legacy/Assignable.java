/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.util.Collection;

/**
 * @author Administrator
 */
public class Assignable {
	private String id;
	private Collection categories;

	public Collection getCategories() {
		return categories;
	}

	public String getId() {
		return id;
	}

	public void setCategories(Collection collection) {
		categories = collection;
	}

	public void setId(String string) {
		id = string;
	}

}
