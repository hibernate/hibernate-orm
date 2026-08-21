/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "author")
public class AuthorAnnotated {
	@Id
	private int id;

	@Column(nullable = false)
	private String name;

	@OneToMany
	@JoinColumn(name = "author_id", referencedColumnName = "id")
	private Set<BookSubselectView> books;

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

	public Set<BookSubselectView> getBooks() {
		return books;
	}

	public void setBooks(Set<BookSubselectView> books) {
		this.books = books;
	}
}
