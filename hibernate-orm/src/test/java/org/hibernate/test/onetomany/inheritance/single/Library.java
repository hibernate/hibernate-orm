/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany.inheritance.single;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;


@Entity
@Table(name="INVENTORYSG")
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
