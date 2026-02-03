/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot.discovery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name="libraries")
public class Library {
	@Id
	private Integer id;
	private String name;
	@ManyToOne
	@JoinColumn(name="address_fk")
	private Address address;
	@OneToMany
	@JoinTable(name="library_books",
			joinColumns = @JoinColumn(name="library_fk"),
			inverseJoinColumns = @JoinColumn(name="book_fk")
	)
	private List<Book> books;

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public List<Book> getBooks() {
		return books;
	}

	public void setBooks(List<Book> books) {
		this.books = books;
	}
}
