/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect;

import java.util.Set;

/**
 * @author Andrea Boriero
 */
public class Author {
	int id;
	String name;
	Set<String> books;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getBooks() {
		return books;
	}

	public void setBooks(Set<String> books) {
		this.books = books;
	}
}
