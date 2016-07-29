package org.hibernate.test.onetomany.inheritance.perclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.test.onetomany.inheritance.Book;
import org.hibernate.test.onetomany.inheritance.Library;

@Entity
@Table(name="BOOKTABPC")
@Access(AccessType.FIELD)
public class BookImpl extends ProductImpl implements Book {

	private String isbn;
	
	@ManyToOne(targetEntity=org.hibernate.test.onetomany.inheritance.perclass.LibraryImpl.class)
	private Library library;
	
	public BookImpl() {
		super();
	}
	
	public BookImpl(String inventoryCode, String isbn, Library inventory) {
		super(inventoryCode);
		this.isbn = isbn;
		setLibrary(inventory);
	}
	
	@Override
	public String getIsbn() {
		return isbn;
	}
	
	@Override
	public Library getLibrary() {
		return library;
	}
	
	@Override
	public void setLibrary(Library arg) {
		if (this.library == arg) {
			return;
		}
		if (this.library != null) {
			library.getBooksOnInventory().remove(getInventoryCode());
		}
		if (arg != null) {
			arg.getBooksOnInventory().put(getInventoryCode(), this);
		}
		this.library = arg;
	}
}
