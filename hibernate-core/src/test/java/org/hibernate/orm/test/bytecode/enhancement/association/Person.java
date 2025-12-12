/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.Set;


@Entity
class Person {

	@OneToMany(mappedBy = "author")
	private Set<Book> booksWritten;

	@OneToMany(mappedBy = "translator")
	private Set<Book> booksTranslated;

	public Set<Book> getBooksWritten() {
		return this.booksWritten;
	}

	public void setBooksWritten(Set<Book> booksWritten){
		this.booksWritten = booksWritten;
	}

	public Set<Book> getBooksTranslated() {
		return this.booksTranslated;
	}

	public void setBooksTranslated(Set<Book> booksTranslated){
		this.booksTranslated = booksTranslated;
	}
}
