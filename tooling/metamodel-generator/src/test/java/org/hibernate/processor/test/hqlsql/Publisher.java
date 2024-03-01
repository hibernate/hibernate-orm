package org.hibernate.processor.test.hqlsql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Publisher {
    @Id Long id;
    String name;
    Address address;
}