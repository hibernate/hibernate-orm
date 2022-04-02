package org.hibernate.jpamodelgen.test.generics;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.concurrent.atomic.DoubleAccumulator;

@MappedSuperclass
public class GenericWithExtendsChild extends GenericWithExtendsSuper<Integer, DoubleAccumulator> {
    @Column(name = "childColumn")
    private String childColumn;
}
