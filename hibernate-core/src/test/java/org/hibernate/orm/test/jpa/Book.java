package org.hibernate.orm.test.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="TYPE_BOOK")
@DiscriminatorValue("MAIN")
@DynamicInsert
public class Book {
    @Id
    public String isbn;
    
    public String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE_BOOK", insertable = false, updatable = false, nullable = false)
    protected BookType bookType;

    public Book() {
    }

    public Book(String isbn, String title) {
        this.isbn = isbn;
        this.title = title;
    }
}