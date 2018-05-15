/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.jpa.test.graphs.named.subgraph;

import java.util.Set;


/**
 * @author Etienne Miret
 */
public class Book {

	private Long id;

	private String isbn;

	private String title;

	private Set<Author> authors;

	private Set<Prize> prizes;

	public Book() {
		super();
	}

	public Long getId() {
		return id;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public Set<Prize> getPrizes() {
		return prizes;
	}

	public void setPrizes(Set<Prize> prizes) {
		this.prizes = prizes;
	}

}
