package org.hibernate.processor.test.data.eg;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface Bookshop extends CrudRepository<Book,String> {
    @Find
    List<Book> byPublisher(String publisher_name);
}
