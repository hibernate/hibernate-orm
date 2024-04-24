package org.hibernate.processor.test.data.reactive;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.Set;

@Entity
public class Author {
    @Id
    String ssn;

    @Basic(optional = false)
    String name;

    Address address;

    @ManyToMany
    Set<Book> books;
}

