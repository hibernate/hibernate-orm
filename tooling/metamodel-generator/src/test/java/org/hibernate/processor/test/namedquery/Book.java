/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQuery;

@Entity
@NamedEntityGraph(name = "entityGraph")
@NamedEntityGraph(attributeNodes = @NamedAttributeNode("text"))
@NamedQuery(name="booksByTitle",
		query = "from Book where title = ?1")
@NamedQuery(name="booksByTitleVerbose",
		query = "select book from Book book where book.title = ?1")
@NamedQuery(name = "titlesWithIsbns",
		query = "select title, isbn from Book")
@NamedQuery(name = "#findByTitle",
		query = "from Book where title like :titlePattern")
@NamedQuery(name = "#findByTitleAndType",
		query = "select book from Book book where book.title like :titlePattern and book.type = :type")
@NamedQuery(name = "#getTitles",
		query = "select title from Book")
@NamedQuery(name = "titlesAndIsbnsAsRecord",
		query = "select new org.hibernate.processor.test.namedquery.TitleAndIsbn(title,isbn) from Book")
@NamedQuery(name = "#getUpperLowerTitles",
		query = "select upper(title), lower(title), length(title) from Book")
@NamedQuery(name = "#typeOfBook",
		query = "select type from Book where isbn = :isbn")
@NamedQuery(name = "#crazy",
		query = "select 1 where :x = :y")
public class Book {
	@Id String isbn;
	String title;
	String text;
	Type type = Type.Book;
}
