/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQuery;

@Entity
@NamedEntityGraph(name = "entityGraph")
@NamedQuery(name = "#findByTitle",
        query = "from Book where title like :titlePattern")
@NamedQuery(name = "#findByTitleAndType",
        query = "select book from Book book where book.title like :titlePattern and book.type = :type")
@NamedQuery(name = "#getTitles",
        query = "select title from Book")
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
