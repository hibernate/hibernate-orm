package org.hibernate.processor.test.superdao.generic;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.Pattern;

import java.util.List;

public interface SuperDao<T,K> {

    EntityManager em();

    @Find
    T get(K isbn);

    @Find
    List<T> books1(@Pattern String title);

    @HQL("where title like :title")
    List<T> books2(String title);

}
