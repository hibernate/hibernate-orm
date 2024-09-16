/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;

@Entity
public class Book {
	@Id String isbn;
	@NaturalId String title;
	@NaturalId String author;
	String text;
	int pages;
	Type type;

	enum Type { Book, Magazine, Journal }
}
