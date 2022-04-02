package org.hibernate.jpamodelgen.test.generics;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TwoClildrenSuperclass<T, S> {

    @EmbeddedId
    private Long key;

    @Column(name = "value")
    private T value;

    @Column(name = "foo")
    private S foo;

}