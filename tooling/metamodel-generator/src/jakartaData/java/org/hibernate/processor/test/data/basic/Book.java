/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.basic;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "books")
public class Book {
    @Id
    String isbn;

    @NaturalId
    @Basic(optional = false)
    String title;

    @Basic(optional = false)
    String text;

    @NaturalId
    LocalDate publicationDate;

    @ManyToMany(mappedBy = "books")
    Set<Author> authors;

    BigDecimal price;

    int pages;

    public Book(String isbn, String title, String text) {
        this.isbn = isbn;
        this.title = title;
        this.text = text;
    }
    Book() {}
}
