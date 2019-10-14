/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session.runtime.check;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Book {

	@Id
	private Integer id;
	private String isbn;
	private String title;
	private String author;
	private Integer copies;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Integer getCopies() {
		return copies;
	}

	public void setCopies(Integer copies) {
		this.copies = copies;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		Book book = (Book) o;
		return Objects.equals( getId(), book.getId() ) &&
				Objects.equals( getIsbn(), book.getIsbn() ) &&
				Objects.equals( getTitle(), book.getTitle() ) &&
				Objects.equals( getAuthor(), book.getAuthor() ) &&
				Objects.equals( getCopies(), book.getCopies() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, isbn, title, author, copies );
	}
}
