/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.testing.orm.domain.library;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * @author Steve Ebersole
 */
@Entity
public class Book {
	@Id
	private Integer id;
	private String name;
	private String isbn;

	@ManyToMany
	@JoinTable( name = "book_authors",
			joinColumns = @JoinColumn(name = "book_fk"),
			inverseJoinColumns = @JoinColumn(name = "author_fk")
	)
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE)
	Set<Person> authors;

	@ManyToMany
	@JoinTable( name = "book_editors",
			joinColumns = @JoinColumn(name = "book_fk"),
			inverseJoinColumns = @JoinColumn(name = "editor_fk")
	)
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE)
	Set<Person> editors;

	protected Book() {
		// for Hibernate use
	}

	public Book(Integer id, String name) {
		this.id = id;
		this.name = name;
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

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public Set<Person> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Person> authors) {
		this.authors = authors;
	}

	public void addAuthor(Person author) {
		if ( authors == null ) {
			authors = new HashSet<>();
		}
		authors.add( author );
	}

	public Set<Person> getEditors() {
		return editors;
	}

	public void setEditors(Set<Person> editors) {
		this.editors = editors;
	}

	public void addEditor(Person editor) {
		if ( editors == null ) {
			editors = new HashSet<>();
		}
		editors.add( editor );
	}
}
