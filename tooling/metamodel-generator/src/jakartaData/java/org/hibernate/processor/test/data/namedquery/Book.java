package org.hibernate.processor.test.data.namedquery;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "books")
public class Book {
    public enum Type { Book, Magazine, Journal }

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

    Type type;

    public Book(String isbn, String title, String text) {
        this.isbn = isbn;
        this.title = title;
        this.text = text;
    }
    Book() {}
}