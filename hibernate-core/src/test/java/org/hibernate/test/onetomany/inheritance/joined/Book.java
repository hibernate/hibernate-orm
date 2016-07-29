package org.hibernate.test.onetomany.inheritance.joined;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="BOOKTABJO")
public class Book extends Product {

	private String isbn;
	
	@ManyToOne
	private Library library;
	
	public Book() {
		super();
	}
	
	public Book(String inventoryCode, String isbn) {
		super(inventoryCode);
		this.isbn = isbn;
	}

	public String getIsbn() {
		return isbn;
	}

	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}
}
