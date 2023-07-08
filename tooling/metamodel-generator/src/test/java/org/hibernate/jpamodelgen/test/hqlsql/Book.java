package org.hibernate.jpamodelgen.test.hqlsql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;

@Entity
public class Book {
    @Id String isbn;
    @NaturalId String title;
    String text;
    @NaturalId String authorName;
}
