package org.hibernate.test.onetomany.inheritance.single;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.test.onetomany.inheritance.Book;
import org.hibernate.test.onetomany.inheritance.Library;

@Entity
@Table(name="INVENTORYSG")
@Access(AccessType.FIELD)
public class LibraryImpl implements Library {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int entid;

	@OneToMany(mappedBy="library", targetEntity=org.hibernate.test.onetomany.inheritance.single.BookImpl.class)
	@MapKey(name="inventoryCode")
	private Map<String,Book> booksOnInventory = new HashMap<String,Book>();
	
	public LibraryImpl() {
		
	}
	
	@Override
	public int getEntid() {
		return entid;
	}
	
	@Override
	public Map<String,Book> getBooksOnInventory() {
		return booksOnInventory;
	}
}
