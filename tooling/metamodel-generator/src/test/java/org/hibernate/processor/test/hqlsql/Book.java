package org.hibernate.processor.test.hqlsql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Book {
    @Id String isbn;
    @NaturalId String title;
    String text;
    @NaturalId String authorName;
    @ManyToOne Publisher publisher;
    BigDecimal price;
    int pages;
    LocalDate publicationDate;
}
