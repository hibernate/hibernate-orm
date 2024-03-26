package org.hibernate.processor.test.data.superdao;

import jakarta.data.repository.Repository;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.Pattern;

import java.util.List;

@Repository
public interface SuperRepo {
    @Find
    List<Book> books1(@Pattern String title);

    @HQL("where title like :title")
    List<Book> books2(String title);
}
