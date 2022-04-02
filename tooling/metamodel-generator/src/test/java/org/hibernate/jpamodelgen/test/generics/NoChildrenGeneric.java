package org.hibernate.jpamodelgen.test.generics;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public abstract class NoChildrenGeneric<T> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @MapKey(name = "timestamp")
    private Map<LocalDateTime, String> mapTest = new HashMap<>();

    @OneToMany
    private List<Integer> listTest = new ArrayList<>();
}
