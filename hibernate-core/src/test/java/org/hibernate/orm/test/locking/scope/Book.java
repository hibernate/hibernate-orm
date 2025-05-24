/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "books")
public class Book {
	@Id
	private Integer id;
	private String title;
	private String synopsis;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "publisher_fk")
	private Publisher publisher;
	@ManyToMany
	@JoinTable(name = "book_authors",
			joinColumns = @JoinColumn(name = "book_fk"),
			inverseJoinColumns = @JoinColumn(name = "author_fk"))
	private Set<Person> authors;
	@ElementCollection
	@CollectionTable(name = "book_tags", joinColumns = @JoinColumn(name = "book_fk"))
	@Column(name = "tag")
	private Set<String> tags;

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

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public void addTag(String tag) {
		if ( tags == null ) {
			tags = new HashSet<>();
		}
		tags.add( tag );
	}
}
