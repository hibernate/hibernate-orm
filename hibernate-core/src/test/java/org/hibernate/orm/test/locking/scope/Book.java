/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity
public class Book {
	@Id
	private Integer id;
	private String title;
	private String synopsis;
	@ManyToOne
	@JoinColumn(name = "publisher_fk")
	private Publisher publisher;
	@ManyToMany
	@JoinTable(name = "book_authors",
			joinColumns = @JoinColumn(name = "book_fk"),
			inverseJoinColumns = @JoinColumn(name = "author_fk"))
	private Set<Person> authors;

	protected Book() {
		// for Hibernate use
	}

	public Book(Integer id, String title, String synopsis) {
		this( id, title, synopsis, null );
	}

	public Book(Integer id, String title, String synopsis, Publisher publisher) {
		this.id = id;
		this.title = title;
		this.synopsis = synopsis;
		this.publisher = publisher;
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSynopsis() {
		return synopsis;
	}

	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}

	public Publisher getPublisher() {
		return publisher;
	}

	public void setPublisher(Publisher publisher) {
		this.publisher = publisher;
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
}
