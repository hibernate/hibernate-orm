package org.hibernate.processor.test.hhh17661;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;

@MappedSuperclass
public abstract class Entity implements Serializable {

    @Id
    private Long id;

}