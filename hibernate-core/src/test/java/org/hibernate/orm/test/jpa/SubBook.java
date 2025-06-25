package org.hibernate.orm.test.jpa;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SUB")
public class SubBook extends Book {

    public SubBook() {
        super();
    }

    public SubBook(String isbn, String title) {
        super(isbn, title);
    }
}