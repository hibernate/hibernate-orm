package org.hibernate.processor.test.data.eg;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;

import java.util.List;

@Repository
public interface Bookshop extends CrudRepository<Book,String> {
    @Find
    @Transactional
    List<Book> byPublisher(String publisher_name);

    @Query("select isbn where title like ?1 order by isbn")
    String[] ssns(String title);

    @Query("select count(this) where title like ?1 order by isbn")
    long count1(String title);

    @Query("select count(this) where this.title like ?1 order by this.isbn")
    long count2(String title);
}
