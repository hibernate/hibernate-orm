/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "publishers")
public class Publisher {
	@Id
	private Integer id;
	private String name;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="editor_fk")
	private Person leadEditor;
	@OneToMany(mappedBy = "publisher")
	private Set<Book> books;

	protected Publisher() {
		// for Hibernate use
	}

	public Publisher(Integer id, String name) {
		this( id, name, null );
	}

	public Publisher(Integer id, String name, Person leadEditor) {
		this.id = id;
		this.name = name;
		this.leadEditor = leadEditor;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Person getLeadEditor() {
		return leadEditor;
	}

	public void setLeadEditor(Person leadEditor) {
		this.leadEditor = leadEditor;
	}

	public Set<Book> getBooks() {
		return books;
	}

	public void setBooks(Set<Book> books) {
		this.books = books;
	}
}
