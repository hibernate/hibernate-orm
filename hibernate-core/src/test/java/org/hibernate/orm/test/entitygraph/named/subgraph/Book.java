/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.subgraph;

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
