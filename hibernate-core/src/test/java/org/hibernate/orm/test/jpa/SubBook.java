package org.hibernate.orm.test.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.hibernate.annotations.DynamicInsert;

@Entity
@DiscriminatorValue("SUB")
@DynamicInsert
public class SubBook extends Book {

    @Column
    public String subdata;

    public SubBook() {
    }
    
    public SubBook(String isbn, String title, String subdata) {
        this.isbn = isbn;
        this.title = title;
        this.subdata = subdata;
    }
}
