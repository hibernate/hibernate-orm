package org.hibernate.jpamodelgen.test.namedquery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;

@Entity
@NamedEntityGraph(name = "entityGraph")
public class Book {
    @Id String isbn;
    String title;
    String text;
}
