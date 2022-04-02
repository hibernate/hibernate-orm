package org.hibernate.jpamodelgen.test.generics;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;

@MappedSuperclass
public class GenericWithExtendsSuper<T extends Number, S extends Number & Serializable> {

    @EmbeddedId
    private Long key;

    @Column(name = "value")
    private S value;

    @Column(name = "foo")
    private T foo;

}
