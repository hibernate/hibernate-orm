package org.hibernate.test.onetomany.inheritance;

import javax.persistence.InheritanceType;

/**
 * Factory class for the different implementation types. 
 * 
 * Copyright © Serisys Limited 2013-2016
 * @author RichardB
 *
 * Jul 29, 2016
 */
public class EntityFactory {
	private final InheritanceType inheritanceType;
	
	public EntityFactory(InheritanceType inheritanceType) {
		this.inheritanceType = inheritanceType;
	}
	
	public String bookPredicate() {
		switch (inheritanceType) {
		case JOINED:
			return "FROM org.hibernate.test.onetomany.inheritance.joined.BookImpl";
		case SINGLE_TABLE:
			return "FROM org.hibernate.test.onetomany.inheritance.single.BookImpl";
		case TABLE_PER_CLASS:
			return "FROM org.hibernate.test.onetomany.inheritance.perclass.BookImpl";
		default:
			return null;
		}
	}
	
	public String libraryPredicate() {
		switch (inheritanceType) {
		case JOINED:
			return "FROM org.hibernate.test.onetomany.inheritance.joined.LibraryImpl";
		case SINGLE_TABLE:
			return "FROM org.hibernate.test.onetomany.inheritance.single.LibraryImpl";
		case TABLE_PER_CLASS:
			return "FROM org.hibernate.test.onetomany.inheritance.perclass.LibraryImpl";
		default:
			return null;
		}
	}

	public Book newBook(String inventoryCode, String isbn, Library inventory) {
		switch (inheritanceType) {
		case JOINED:
			return new org.hibernate.test.onetomany.inheritance.joined.BookImpl(inventoryCode, isbn, inventory);
		case SINGLE_TABLE:
			return new org.hibernate.test.onetomany.inheritance.single.BookImpl(inventoryCode, isbn, inventory);
		case TABLE_PER_CLASS:
			return new org.hibernate.test.onetomany.inheritance.perclass.BookImpl(inventoryCode, isbn, inventory);
		default:
			return null;
		}
	}

	public Library newLibrary() {
		switch (inheritanceType) {
		case JOINED:
			return new org.hibernate.test.onetomany.inheritance.joined.LibraryImpl();
		case SINGLE_TABLE:
			return new org.hibernate.test.onetomany.inheritance.single.LibraryImpl();
		case TABLE_PER_CLASS:
			return new org.hibernate.test.onetomany.inheritance.perclass.LibraryImpl();
		default:
			return null;
		}
	}
}
