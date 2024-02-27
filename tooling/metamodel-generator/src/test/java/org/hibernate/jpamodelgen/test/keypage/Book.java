package org.hibernate.jpamodelgen.test.keypage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpamodelgen.test.hqlsql.Publisher;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Book {
    @Id String isbn;
    @NaturalId String title;
    String text;
    @NaturalId String authorName;
    @ManyToOne
	Publisher publisher;
    BigDecimal price;
    int pages;
    LocalDate publicationDate;
}
