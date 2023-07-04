package org.hibernate.jpamodelgen.test.querymethod;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;

@Entity
public class Book {
    @Id String isbn;
    String title;
    String text;
}
