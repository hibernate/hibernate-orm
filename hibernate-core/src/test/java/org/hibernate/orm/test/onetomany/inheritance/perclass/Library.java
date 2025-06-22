/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany.inheritance.perclass;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="LIBRARYPC")
public class Library {

	@Id
	@GeneratedValue
	private int entid;

	@OneToMany(mappedBy="library", cascade = CascadeType.ALL)
	@MapKey(name="inventoryCode")
	private Map<String,Book> booksOnInventory = new HashMap<>();

	@OneToMany(mappedBy="library", cascade = CascadeType.ALL)
	@MapKey(name="isbn")
	private Map<String,Book> booksOnIsbn = new HashMap<>();

	public int getEntid() {
		return entid;
	}

	public Map<String,Book> getBooksOnInventory() {
		return booksOnInventory;
	}

	public Map<String, Book> getBooksOnIsbn() {
		return booksOnIsbn;
	}

	public void addBook(Book book) {
		book.setLibrary( this );
		booksOnInventory.put( book.getInventoryCode(), book );
		booksOnIsbn.put( book.getIsbn(), book );
	}

	public void removeBook(Book book) {
		book.setLibrary( null );
		booksOnInventory.remove( book.getInventoryCode() );
		booksOnIsbn.remove( book.getIsbn() );
	}
}
